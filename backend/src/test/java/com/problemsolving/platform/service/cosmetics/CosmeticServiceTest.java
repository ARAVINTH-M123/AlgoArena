package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.api.dto.cosmetics.CosmeticItemDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticsDto;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
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
    void thereIsExactlyOneThemeAndOneBorderPerRank() {
        List<CosmeticCatalog.Item> all = CosmeticCatalog.all();
        assertEquals(20, all.size());
        Set<String> keys = new HashSet<>();
        for (CosmeticCategory category : List.of(CosmeticCategory.THEME, CosmeticCategory.BORDER)) {
            Set<Integer> ranks = new HashSet<>();
            for (CosmeticCatalog.Item item : all) {
                if (item.category() == category) {
                    assertTrue(ranks.add(item.unlockRank()), "two " + category + " cosmetics for rank " + item.unlockRank());
                    assertTrue(keys.add(item.key()), "duplicate key " + item.key());
                }
            }
            assertEquals(10, ranks.size());
        }
        assertEquals(20, service.catalog().size());
        assertEquals("Event Horizon", CosmeticCatalog.forRank(CosmeticCategory.THEME, 9).displayName());
        assertEquals("Prismatic Gold", CosmeticCatalog.forRank(CosmeticCategory.BORDER, 9).displayName());
    }

    @Test
    void ranksUnlockTheirCosmeticsAndReportThemOnRankUp() {
        List<CosmeticCatalog.Item> unlocked = CosmeticCatalog.unlockedAtRanks(List.of(1, 2));
        assertEquals(4, unlocked.size());
        Set<String> keys = new HashSet<>();
        for (CosmeticCatalog.Item item : unlocked) keys.add(item.key());
        assertTrue(keys.contains("theme.ember") && keys.contains("border.ember-glow"));
        assertTrue(keys.contains("theme.terminal") && keys.contains("border.green-pixel"));
        assertEquals(0, CosmeticCatalog.unlockedAtRanks(List.of()).size());
    }

    // -------------------------------------------------------------- ownership

    @Test
    void aNewcomerOwnsOnlyTheRankZeroCosmetics() {
        CosmeticsDto dto = service.mine("alice");
        assertEquals(Set.of("theme.void", "border.thin-gray"), ownedKeys(dto));
        assertEquals("theme.void", dto.themeKey());
        assertEquals("border.thin-gray", dto.borderKey());
        assertEquals(null, dto.pinnedThemeKey());
        assertEquals(null, dto.pinnedBorderKey());
        assertEquals(20, dto.items().size());
        assertEquals(0, store.nationCount());                  // looking creates nothing
    }

    @Test
    void reachingARankUnlocksEverythingUpToIt() {
        reachRank(4);                                          // Kernel
        CosmeticsDto dto = service.mine("alice");
        assertEquals(10, ownedKeys(dto).size());               // ranks 0..4, theme + border
        assertTrue(ownedKeys(dto).contains("theme.stonework"));
        assertFalse(ownedKeys(dto).contains("theme.brickline"));
        assertEquals("theme.stonework", dto.themeKey());       // automatic: the current rank's
        assertEquals("border.stone", dto.borderKey());
    }

    // ------------------------------------------------------------------ equip

    @Test
    void anOwnedCosmeticCanBePinnedAndStaysWhenTheRankGrows() {
        reachRank(4);
        CosmeticsDto dto = equip("THEME", "theme.timber");
        assertEquals("theme.timber", dto.pinnedThemeKey());
        assertEquals("theme.timber", dto.themeKey());
        assertEquals("border.stone", dto.borderKey());          // the other category is untouched

        reachRank(6);                                           // rank up: pinned theme stays, border follows the rank
        CosmeticsDto later = service.mine("alice");
        assertEquals("theme.timber", later.themeKey());
        assertEquals("border.white-mist", later.borderKey());
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
        assertEquals(null, service.mine("alice").pinnedThemeKey());
    }

    @Test
    void unknownKeysWrongCategoriesAndUnsupportedCategoriesAreRejected() {
        reachRank(9);
        assertEquals("400 Unknown cosmetic", errorOf(() -> equip("THEME", "theme.nope")));
        assertEquals("400 Unknown cosmetic", errorOf(() -> equip("THEME", "border.stone")));      // key belongs to another category
        assertEquals("400 That category cannot be equipped yet", errorOf(() -> equip("ANTHEM", "x")));
        assertEquals("400 Unknown cosmetic category", errorOf(() -> equip("banana", "theme.void")));
        assertEquals("400 Unknown cosmetic category", errorOf(() -> equip(null, "theme.void")));
    }

    @Test
    void theEquippedFlagMarksTheCosmeticsInUse() {
        reachRank(2);
        CosmeticsDto dto = equip("THEME", "theme.void");
        for (CosmeticItemDto item : dto.items()) {
            boolean inUse = item.key().equals("theme.void") || item.key().equals("border.green-pixel");
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
    }
}
