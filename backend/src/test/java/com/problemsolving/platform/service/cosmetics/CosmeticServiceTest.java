package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.api.dto.cosmetics.CosmeticItemDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticsDto;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
import com.problemsolving.platform.api.dto.cosmetics.FlagDto;
import com.problemsolving.platform.api.dto.cosmetics.FlagRequest;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.enums.CosmeticCategory;
import com.problemsolving.platform.service.nation.InMemoryNationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticServiceTest {

    private static final Long USER = 1L;
    private static final Instant T0 = Instant.parse("2026-03-01T12:00:00Z");

    private InMemoryNationStore store;
    private CosmeticService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryNationStore();
        store.registerUser(USER, "alice");
        service = new CosmeticService(store, new NationProperties());
    }

    // ---------------------------------------------------------------- helpers

    private void reachRank(int rank) {
        store.lockOrCreateNation(USER).setRankIndex(rank);
    }

    private EquipRequest request(String category, String key) {
        EquipRequest r = new EquipRequest();
        r.setCategory(category);
        r.setKey(key);
        return r;
    }

    private CosmeticsDto equip(String category, String key) {
        return service.equip("alice", request(category, key), T0);
    }

    private String errorOf(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException ex) {
            return ex.getStatusCode().value() + " " + ex.getReason();
        }
        throw new AssertionError("expected an error response");
    }

    private Set<String> ownedKeys(CosmeticsDto dto) {
        Set<String> keys = new HashSet<>();
        for (CosmeticItemDto item : dto.items()) {
            if (item.owned()) keys.add(item.key());
        }
        return keys;
    }

    // ---------------------------------------------------------------- catalog

    @Test
    void thereIsExactlyOneThemeBorderAnthemAndEmblemPerRank() {
        List<CosmeticCatalog.Item> all = CosmeticCatalog.all();
        assertEquals(72, all.size());                      // 40 rank + 7 patterns + 12 colors + 12 cache pool + 1 Ascended
        Set<String> keys = new HashSet<>();
        for (CosmeticCategory category : List.of(CosmeticCategory.THEME, CosmeticCategory.BORDER,
                CosmeticCategory.ANTHEM, CosmeticCategory.FLAG_EMBLEM)) {
            Set<Integer> ranks = new HashSet<>();
            for (CosmeticCatalog.Item item : all) {
                if (item.category() == category) {
                    assertTrue(ranks.add(item.unlockRank()), "two " + category + " cosmetics for rank " + item.unlockRank());
                    assertTrue(keys.add(item.key()), "duplicate key " + item.key());
                }
            }
            assertEquals(10, ranks.size());
        }
        Set<Integer> patternRanks = new HashSet<>();
        Set<Integer> colorRanks = new HashSet<>();
        for (CosmeticCatalog.Item item : all) {
            if (item.category() == CosmeticCategory.FLAG_PATTERN) patternRanks.add(item.unlockRank());
            if (item.category() == CosmeticCategory.FLAG_COLOR) colorRanks.add(item.unlockRank());
        }
        assertEquals(Set.of(0), patternRanks);             // flag parts are owned from the start
        assertEquals(Set.of(0), colorRanks);
        assertEquals(72, service.catalog().size());
        assertEquals("Event Horizon", CosmeticCatalog.forRank(CosmeticCategory.THEME, 9).displayName());
        assertEquals("Prismatic Gold", CosmeticCatalog.forRank(CosmeticCategory.BORDER, 9).displayName());
        assertEquals("Ascension", CosmeticCatalog.forRank(CosmeticCategory.ANTHEM, 9).displayName());
        assertEquals("Spiral", CosmeticCatalog.forRank(CosmeticCategory.FLAG_EMBLEM, 9).displayName());
    }

    @Test
    void ranksUnlockTheirCosmeticsAndReportThemOnRankUp() {
        List<CosmeticCatalog.Item> unlocked = CosmeticCatalog.unlockedAtRanks(List.of(1, 2));
        assertEquals(8, unlocked.size());                  // 4 per rank: theme, border, anthem, emblem
        Set<String> keys = new HashSet<>();
        for (CosmeticCatalog.Item item : unlocked) keys.add(item.key());
        assertTrue(keys.contains("theme.ember") && keys.contains("border.ember-glow"));
        assertTrue(keys.contains("theme.terminal") && keys.contains("border.green-pixel"));
        assertTrue(keys.contains("anthem.campfire") && keys.contains("emblem.seedling"));
        assertTrue(keys.contains("anthem.chiptune") && keys.contains("emblem.gear"));
        assertEquals(0, CosmeticCatalog.unlockedAtRanks(List.of()).size());
    }

    // -------------------------------------------------------------- ownership

    @Test
    void aNewcomerOwnsOnlyTheRankZeroCosmetics() {
        CosmeticsDto dto = service.mine("alice");
        assertEquals(23, ownedKeys(dto).size());   // 4 rank-0 cosmetics + 7 patterns + 12 colors
        assertTrue(ownedKeys(dto).containsAll(Set.of(
                "theme.void", "border.thin-gray", "anthem.soft-pulse", "emblem.cursor",
                "pattern.horizontal", "pattern.canton", "color.slate", "color.charcoal", "color.pink")));
        assertEquals("theme.void", dto.themeKey());
        assertEquals("border.thin-gray", dto.borderKey());
        assertEquals("anthem.soft-pulse", dto.anthemKey());
        assertNull(dto.pinnedThemeKey());
        assertNull(dto.pinnedAnthemKey());
        assertEquals(72, dto.items().size());
        assertEquals(null, dto.skyKey());                      // scene cosmetics: none equipped by default
        assertEquals(null, dto.auraKey());
        assertEquals(null, dto.ambientKey());
        assertEquals(0, store.nationCount());                  // looking creates nothing
    }

    @Test
    void reachingARankUnlocksEverythingUpToIt() {
        reachRank(4);                                          // Kernel
        CosmeticsDto dto = service.mine("alice");
        assertEquals(39, ownedKeys(dto).size());               // ranks 0..4 x 4 + 7 patterns + 12 colors
        assertTrue(ownedKeys(dto).contains("theme.stonework"));
        assertFalse(ownedKeys(dto).contains("theme.brickline"));
        assertTrue(ownedKeys(dto).contains("anthem.march"));
        assertFalse(ownedKeys(dto).contains("anthem.industrial"));
        assertEquals("theme.stonework", dto.themeKey());       // automatic: the current rank's
        assertEquals("border.stone", dto.borderKey());
        assertEquals("anthem.march", dto.anthemKey());
    }

    // ------------------------------------------------------------------ equip

    @Test
    void anOwnedCosmeticCanBePinnedAndStaysWhenTheRankGrows() {
        reachRank(4);
        CosmeticsDto dto = equip("THEME", "theme.timber");
        assertEquals("theme.timber", dto.pinnedThemeKey());
        assertEquals("theme.timber", dto.themeKey());
        assertEquals("border.stone", dto.borderKey());          // the other categories are untouched
        assertEquals("anthem.march", dto.anthemKey());

        reachRank(6);                                           // rank up: pinned theme stays, border follows the rank
        CosmeticsDto later = service.mine("alice");
        assertEquals("theme.timber", later.themeKey());
        assertEquals("border.white-mist", later.borderKey());
        assertEquals("anthem.airy", later.anthemKey());
    }

    @Test
    void autoGoesBackToTheCosmeticOfTheCurrentRank() {
        reachRank(4);
        equip("THEME", "theme.timber");
        assertEquals("theme.stonework", equip("THEME", "auto").themeKey());
        equip("BORDER", "border.wood-grain");
        assertEquals("border.stone", equip("border", "").borderKey());     // empty key and lower-case category work too
        assertEquals(null, service.mine("alice").pinnedBorderKey());
    }

    @Test
    void lockedCosmeticsCannotBeEquipped() {
        reachRank(1);
        assertEquals("400 Locked: reach rank Quantum to unlock Aurora", errorOf(() -> equip("THEME", "theme.aurora")));
        assertEquals("400 Locked: reach rank Singularity to unlock Ascension",
                errorOf(() -> equip("ANTHEM", "anthem.ascension")));
        assertEquals(null, service.mine("alice").pinnedThemeKey());
        assertEquals(null, service.mine("alice").pinnedAnthemKey());
    }

    @Test
    void unknownKeysWrongCategoriesAndUnsupportedCategoriesAreRejected() {
        reachRank(9);
        assertEquals("400 Unknown cosmetic", errorOf(() -> equip("THEME", "theme.nope")));
        assertEquals("400 Unknown cosmetic", errorOf(() -> equip("THEME", "border.stone")));      // key belongs to another category
        assertEquals("400 Unknown cosmetic", errorOf(() -> equip("ANTHEM", "emblem.gear")));      // anthem slot wants an anthem
        assertEquals("400 Unknown cosmetic", errorOf(() -> equip("SKY", "aura.ember-ring")));     // sky slot wants a sky
        // flags are changed through the nation profile endpoint, not through /equip
        assertEquals("400 That category cannot be equipped yet", errorOf(() -> equip("FLAG_COLOR", "color.slate")));
        assertEquals("400 Unknown cosmetic category", errorOf(() -> equip("banana", "theme.void")));
        assertEquals("400 Unknown cosmetic category", errorOf(() -> equip(null, "theme.void")));
    }

    @Test
    void theEquippedFlagMarksTheCosmeticsInUse() {
        reachRank(2);
        CosmeticsDto dto = equip("THEME", "theme.void");
        for (CosmeticItemDto item : dto.items()) {
            boolean inUse = item.key().equals("theme.void") || item.key().equals("border.green-pixel")
                    || item.key().equals("anthem.chiptune") || item.key().equals("emblem.gear")
                    || item.key().equals("pattern.horizontal")                   // the default flag parts
                    || item.key().equals("color.slate") || item.key().equals("color.charcoal");
            assertEquals(inUse, item.equipped());
        }
    }

    @Test
    void unknownUsersAreNotFound() {
        try {
            service.mine("ghost");
            throw new AssertionError("expected 404");
        } catch (ResponseStatusException ex) {
            assertEquals(404, ex.getStatusCode().value());
        }
    }

    @Test
    void withoutANationTheEffectiveCosmeticsAreRankZero() {
        assertEquals("theme.void", CosmeticService.effectiveKey((Nation) null, CosmeticCategory.THEME));
        assertEquals("border.thin-gray", CosmeticService.effectiveKey((Nation) null, CosmeticCategory.BORDER));
        assertEquals("anthem.soft-pulse", CosmeticService.effectiveKey((Nation) null, CosmeticCategory.ANTHEM));
    }

    // ------------------------------------------------------------- anthem equip (5B)

    @Test
    void anthemsCanBeEquippedPinnedAndResetToAuto() {
        reachRank(4);
        CosmeticsDto dto = equip("ANTHEM", "anthem.campfire");
        assertEquals("anthem.campfire", dto.pinnedAnthemKey());
        assertEquals("anthem.campfire", dto.anthemKey());
        assertEquals("theme.stonework", dto.themeKey());       // other categories untouched

        reachRank(6);                                          // the pinned anthem survives the rank-up
        assertEquals("anthem.campfire", service.mine("alice").anthemKey());
        assertEquals("anthem.airy", equip("anthem", "auto").anthemKey());   // back to automatic: the rank-6 anthem
        assertNull(service.mine("alice").pinnedAnthemKey());
    }

    // ---------------------------------------------------------------- flag validation (5B)

    private FlagRequest flag(String pattern, List<String> colors, String emblem) {
        FlagRequest r = new FlagRequest();
        r.setPattern(pattern);
        r.setColors(colors);
        r.setEmblem(emblem);
        return r;
    }

    @Test
    void validateFlagAcceptsTwoOrThreeColorsAndAnOptionalEmblem() {
        FlagDto two = CosmeticService.validateFlag(0, flag("pattern.cross", List.of("color.crimson", "color.white"), null));
        assertEquals("pattern.cross", two.pattern());
        assertEquals(List.of("color.crimson", "color.white"), two.colors());
        assertNull(two.emblem());

        FlagDto three = CosmeticService.validateFlag(9,
                flag(" pattern.triangle ", List.of(" color.slate ", "color.amber", "color.white "), "emblem.spiral"));
        assertEquals("pattern.triangle", three.pattern());
        assertEquals(List.of("color.slate", "color.amber", "color.white"), three.colors());   // trimmed
        assertEquals("emblem.spiral", three.emblem());
    }

    @Test
    void validateFlagMessagesAreExact() {
        assertEquals("400 Unknown flag pattern", errorOf(() -> CosmeticService.validateFlag(0, flag("pattern.nope", List.of("color.slate", "color.white"), null))));
        assertEquals("400 Unknown flag pattern", errorOf(() -> CosmeticService.validateFlag(0, flag(null, List.of("color.slate", "color.white"), null))));
        assertEquals("400 A flag needs 2 or 3 colors", errorOf(() -> CosmeticService.validateFlag(0, flag("pattern.cross", List.of("color.slate"), null))));
        assertEquals("400 A flag needs 2 or 3 colors", errorOf(() -> CosmeticService.validateFlag(0, flag("pattern.cross", List.of(), null))));
        assertEquals("400 A flag needs 2 or 3 colors", errorOf(() -> CosmeticService.validateFlag(0,
                flag("pattern.cross", List.of("color.slate", "color.white", "color.teal", "color.pink"), null))));
        assertEquals("400 Unknown flag color", errorOf(() -> CosmeticService.validateFlag(0, flag("pattern.cross", List.of("color.slate", "color.gold"), null))));
        assertEquals("400 Unknown flag emblem", errorOf(() -> CosmeticService.validateFlag(0, flag("pattern.cross", List.of("color.slate", "color.white"), "emblem.nope"))));
        assertEquals("400 Locked: reach rank Kernel to unlock Shield",
                errorOf(() -> CosmeticService.validateFlag(0, flag("pattern.cross", List.of("color.slate", "color.white"), "emblem.shield"))));
        // duplicates collapse to one color, which is too few
        assertEquals("400 A flag needs 2 or 3 colors", errorOf(() -> CosmeticService.validateFlag(0,
                flag("pattern.cross", List.of("color.slate", "color.slate"), null))));
        // locked theme keys in the wrong slot stay "unknown"
        assertEquals("400 Unknown flag pattern", errorOf(() -> CosmeticService.validateFlag(0, flag("theme.void", List.of("color.slate", "color.white"), null))));
    }

    @Test
    void theDefaultFlagFollowsTheRankAndShowsForNationsWithoutOne() {
        FlagDto fresh = CosmeticService.defaultFlag(0);
        assertEquals("pattern.horizontal", fresh.pattern());
        assertEquals(List.of("color.slate", "color.charcoal"), fresh.colors());
        assertEquals("emblem.cursor", fresh.emblem());
        assertEquals("emblem.shield", CosmeticService.defaultFlag(4).emblem());

        assertEquals(fresh, CosmeticService.flagOf(null));
        assertEquals(fresh, CosmeticService.flagOf(store.lockOrCreateNation(USER)));   // never customized
        // a malformed stored row (e.g. renamed catalog key) falls back to the default instead of breaking
        Nation broken = store.lockOrCreateNation(USER);
        broken.setFlagPattern("pattern.deleted");
        broken.setFlagColors("color.slate,color.charcoal");
        assertEquals(fresh, CosmeticService.flagOf(broken));
    }

    // ---------------------------------------------------------------- acquired cosmetics (Phase 6)

    @Test
    void cacheCosmeticsNeedAnOwnershipRowBeforeTheyCanBeEquipped() {
        Nation nation = store.lockOrCreateNation(USER);
        assertEquals("400 Locked: Meadow Dawn is not in your collection yet (open caches or craft it)",
                errorOf(() -> equip("SKY", "sky.meadow-dawn")));

        // the /me view marks it unowned (rank cosmetics stay rank-derived; cache cosmetics need the row)
        CosmeticsDto before = service.mine("alice");
        assertTrue(before.items().stream().filter(item -> item.key().equals("sky.meadow-dawn")).allMatch(item -> !item.owned()));

        var owned = new com.problemsolving.platform.domain.entity.NationCosmeticOwned();
        owned.setUserId(USER);
        owned.setCosmeticKey("sky.meadow-dawn");
        owned.setAcquiredVia("CACHE");
        owned.setAcquiredAt(T0);
        store.saveOwnedCosmetic(owned);

        CosmeticsDto after = equip("SKY", "sky.meadow-dawn");
        assertEquals("sky.meadow-dawn", after.skyKey());
        assertEquals("sky.meadow-dawn", after.pinnedSkyKey());
        assertTrue(after.items().stream().filter(item -> item.key().equals("sky.meadow-dawn")).allMatch(item -> item.owned() && item.equipped()));

        // "auto" resets it to none (no rank default exists for scene cosmetics)
        CosmeticsDto reset = equip("SKY", "auto");
        assertNull(reset.skyKey());
        assertNull(reset.pinnedSkyKey());
    }

    @Test
    void skyAuraAndAmbientHaveNoRankDefault() {
        assertEquals(null, CosmeticService.effectiveKey(null, CosmeticCategory.SKY));
        assertEquals(null, CosmeticService.effectiveKey(store.lockOrCreateNation(USER), CosmeticCategory.AURA));
        // ...while the rank categories still fall back to the rank's cosmetic
        assertEquals("theme.void", CosmeticService.effectiveKey(null, CosmeticCategory.THEME));
    }

    @Test
    void anEquippedAuraSurvivesInTheNationView() {
        Nation nation = store.lockOrCreateNation(USER);
        var owned = new com.problemsolving.platform.domain.entity.NationCosmeticOwned();
        owned.setUserId(USER);
        owned.setCosmeticKey("aura.ascended");
        owned.setAcquiredVia("SPECIAL");
        owned.setAcquiredAt(T0);
        store.saveOwnedCosmetic(owned);
        CosmeticsDto dto = equip("AURA", "aura.ascended");
        assertEquals("aura.ascended", dto.auraKey());
        assertEquals("aura.ascended", store.lockOrCreateNation(USER).getEquippedAuraKey());
        assertTrue(dto.items().stream().filter(item -> item.key().equals("aura.ascended")).allMatch(item -> item.owned() && item.equipped()));
    }
}
