package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.api.dto.cosmetics.CacheOpenResultDto;
import com.problemsolving.platform.api.dto.cosmetics.CacheStateDto;
import com.problemsolving.platform.api.dto.cosmetics.CraftResultDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.enums.CosmeticCategory;
import com.problemsolving.platform.domain.enums.CosmeticRarity;
import com.problemsolving.platform.domain.enums.CosmeticSource;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.service.nation.InMemoryNationStore;
import com.problemsolving.platform.service.nation.ScriptedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheServiceTest {

    private static final Long USER = 1L;
    private static final Instant T0 = Instant.parse("2026-03-01T12:00:00Z");

    private InMemoryNationStore store;
    private Nation nation;
    private CacheService cache;

    @BeforeEach
    void setUp() {
        store = new InMemoryNationStore();
        store.registerUser(USER, "alice");
        nation = store.lockOrCreateNation(USER);
    }

    // ---------------------------------------------------------------- helpers

    private CacheService cacheWith(double[] doubles) {
        return new CacheService(store, new ScriptedRandom(doubles));
    }

    private CacheService cacheWith(double[] doubles, int[] ints) {
        return new CacheService(store, new ScriptedRandom(doubles, ints));
    }

    private String errorOf(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException ex) {
            return ex.getStatusCode().value() + " " + ex.getReason();
        }
        throw new AssertionError("expected an error response");
    }

    // ---------------------------------------------------------------- drops

    @Test
    void dropsFollowTheDifficultyRates() {
        CacheService cache = cacheWith(new double[]{0.029, 0.03, 0.05, 0.06, 0.11, 0.12});
        assertTrue(cache.rollDrop(nation, Difficulty.EASY, T0));
        assertFalse(cache.rollDrop(nation, Difficulty.EASY, T0));
        assertTrue(cache.rollDrop(nation, Difficulty.MEDIUM, T0));
        assertFalse(cache.rollDrop(nation, Difficulty.MEDIUM, T0));
        assertTrue(cache.rollDrop(nation, Difficulty.HARD, T0));
        assertFalse(cache.rollDrop(nation, Difficulty.HARD, T0));
        assertEquals(3, nation.getUnopenedCaches());
        assertEquals(1, nation.getSolvesSinceCache());     // the last roll missed and was counted
    }

    @Test
    void theFortiethSolveWithoutADropIsGuaranteed() {
        CacheService cache = cacheWith(new double[]{0.999});
        for (int i = 1; i <= 39; i++) {
            assertFalse(cache.rollDrop(nation, Difficulty.EASY, T0),
                    "solve " + i + " should not drop");
        }
        assertEquals(39, nation.getSolvesSinceCache());
        assertTrue(cache.rollDrop(nation, Difficulty.EASY, T0));   // pity
        assertEquals(1, nation.getUnopenedCaches());
        assertEquals(0, nation.getSolvesSinceCache());
    }

    // ---------------------------------------------------------------- rarity rolls

    @Test
    void rarityRollsCoverAllFourBands() {
        assertEquals(CosmeticRarity.COMMON, CacheService.rarityFromRoll(0, 70, 22, 7));
        assertEquals(CosmeticRarity.COMMON, CacheService.rarityFromRoll(69, 70, 22, 7));
        assertEquals(CosmeticRarity.RARE, CacheService.rarityFromRoll(70, 70, 22, 7));
        assertEquals(CosmeticRarity.RARE, CacheService.rarityFromRoll(91, 70, 22, 7));
        assertEquals(CosmeticRarity.EPIC, CacheService.rarityFromRoll(92, 70, 22, 7));
        assertEquals(CosmeticRarity.EPIC, CacheService.rarityFromRoll(98, 70, 22, 7));
        assertEquals(CosmeticRarity.LEGENDARY, CacheService.rarityFromRoll(99, 70, 22, 7));
        assertEquals(CosmeticRarity.COMMON, CacheService.rarityFromRoll(-5, 70, 22, 7));
        assertEquals(CosmeticRarity.LEGENDARY, CacheService.rarityFromRoll(500, 70, 22, 7));
    }

    @Test
    void openingGrantsAnUnownedCosmeticOfTheRolledRarity() {
        nation.setUnopenedCaches(1);
        // 0.50 -> COMMON; the COMMON pool has 3 unowned items, the second is aura.ember-ring
        CacheService cache = cacheWith(new double[]{0.50}, new int[]{1});
        CacheOpenResultDto result = cache.open("alice", T0);
        assertEquals("COMMON", result.rarity());
        assertEquals("aura.ember-ring", result.key());
        assertFalse(result.wasDuplicate());
        assertEquals(0, result.shardsGained());
        assertEquals(0, result.unopenedCaches());
        assertNotNull(store.owned(USER, "aura.ember-ring"));
        assertEquals("CACHE", store.owned(USER, "aura.ember-ring").getAcquiredVia());
        assertEquals(1, store.cacheLogs(USER).size());
        assertFalse(store.cacheLogs(USER).get(0).isWasDuplicate());
    }

    @Test
    void theTenthOpenWithoutRareOrBetterIsForcedRareOrBetter() {
        nation.setUnopenedCaches(1);
        nation.setOpensSinceRare(9);
        // forced: nextInt(30) = 5 -> RARE band; the RARE pool has 3 unowned items, the first is sky.dusk-fire
        CacheService cache = cacheWith(new double[]{0.999}, new int[]{5, 0});
        CacheOpenResultDto result = cache.open("alice", T0);
        assertEquals("RARE", result.rarity());
        assertEquals("sky.dusk-fire", result.key());
        assertEquals(0, nation.getOpensSinceRare());       // the counter resets after Rare or better
    }

    @Test
    void theRarePityCounterClimbsOnCommonAndResetsOnBetter() {
        nation.setUnopenedCaches(2);
        // first open: 0.10 -> COMMON (counter 1); second open: 0.93 -> EPIC (counter resets)
        CacheService cache = cacheWith(new double[]{0.10, 0.93}, new int[]{0, 0});
        assertEquals("COMMON", cache.open("alice", T0).rarity());
        assertEquals(1, nation.getOpensSinceRare());
        assertEquals("EPIC", cache.open("alice", T0).rarity());
        assertEquals(0, nation.getOpensSinceRare());
    }

    @Test
    void aDuplicateConvertsToShards() {
        // own the whole COMMON pool first: three opens with the first pool index each time
        CacheService granter = cacheWith(new double[]{0.0}, new int[]{0, 0, 0});
        nation.setUnopenedCaches(1);
        granter.open("alice", T0);
        nation.setUnopenedCaches(1);
        granter.open("alice", T0);
        nation.setUnopenedCaches(1);
        granter.open("alice", T0);
        long ownedBefore = store.findOwnedCosmetics(USER).size();

        nation.setUnopenedCaches(1);
        CacheOpenResultDto result = cacheWith(new double[]{0.5}, new int[]{2}).open("alice", T0);
        assertTrue(result.wasDuplicate());
        assertEquals(10, result.shardsGained());           // COMMON duplicate
        assertEquals(10, result.shards());
        assertEquals(ownedBefore, store.findOwnedCosmetics(USER).size());   // nothing new granted
        assertTrue(store.cacheLogs(USER).get(store.cacheLogs(USER).size() - 1).isWasDuplicate());
    }

    @Test
    void openingWithoutACacheIsRejected() {
        assertEquals("400 No Mystery Cache to open - solve problems to earn one",
                errorOf(() -> cacheWith(new double[]{0.5}).open("alice", T0)));
    }

    // ---------------------------------------------------------------- crafting

    @Test
    void craftChargesTheRarityCostAndGrantsTheCosmetic() {
        nation.setShards(400);
        CacheService cache = cacheWith(new double[]{0.5});
        CraftResultDto result = cache.craft("alice", "aura.aurora-crown", T0);      // EPIC -> 400
        assertEquals(400, result.shardsSpent());
        assertEquals(0, result.shardsLeft());
        assertEquals("EPIC", result.rarity());
        assertEquals("AURA", result.category());
        assertEquals("SHARDS", store.owned(USER, "aura.aurora-crown").getAcquiredVia());
    }

    @Test
    void craftNeedsEnoughShards() {
        nation.setShards(399);
        assertEquals("400 Not enough shards: crafting Aurora Crown needs 400, you have 399",
                errorOf(() -> cacheWith(new double[]{0.5}).craft("alice", "aura.aurora-crown", T0)));
        assertEquals(399, nation.getShards());             // nothing was spent
    }

    @Test
    void craftRejectsUnknownNonCacheAndOwnedKeys() {
        nation.setShards(10_000);
        assertEquals("400 Unknown cosmetic",
                errorOf(() -> cacheWith(new double[]{0.5}).craft("alice", "nope", T0)));
        assertEquals("400 Only cache cosmetics can be crafted",
                errorOf(() -> cacheWith(new double[]{0.5}).craft("alice", "theme.void", T0)));
        cacheWith(new double[]{0.5}).craft("alice", "sky.nebula", T0);
        assertEquals("400 You already own Nebula",
                errorOf(() -> cacheWith(new double[]{0.5}).craft("alice", "sky.nebula", T0)));
    }

    // ---------------------------------------------------------------- special grants

    @Test
    void theAscendedAuraIsGrantedExactlyOnce() {
        cacheWith(new double[]{0.5}).grantAscendedAura(USER, T0);
        cacheWith(new double[]{0.5}).grantAscendedAura(USER, T0.plusSeconds(60));   // idempotent
        assertNotNull(store.owned(USER, "aura.ascended"));
        assertEquals("SPECIAL", store.owned(USER, "aura.ascended").getAcquiredVia());
        assertEquals(1, store.findOwnedCosmetics(USER).size());
    }

    // ---------------------------------------------------------------- state for the UI

    @Test
    void theCacheStateReportsCountersAndTheExactOdds() {
        nation.setUnopenedCaches(2);
        nation.setShards(75);
        nation.setSolvesSinceCache(11);
        nation.setOpensSinceRare(4);
        CacheStateDto state = cacheWith(new double[]{0.5}).state("alice");
        assertEquals(2, state.unopenedCaches());
        assertEquals(75, state.shards());
        assertEquals(11, state.solvesSinceCache());
        assertEquals(40, state.solvesForGuarantee());
        assertEquals(List.of(3, 6, 12), state.dropPercent());
        assertEquals(4, state.opensSinceRare());
        assertEquals(10, state.opensForRarePity());
        assertEquals(List.of(70, 22, 7, 1), state.openPercent());
        assertEquals(List.of(10L, 30L, 100L, 300L), state.duplicateShards());
        assertEquals(List.of(40L, 120L, 400L, 1200L), state.craftCost());
    }

    @Test
    void aUserWithoutANationGetsAnEmptyCacheState() {
        store.registerUser(2L, "ghost");
        CacheStateDto state = cacheWith(new double[]{0.5}).state("ghost");
        assertEquals(0, state.unopenedCaches());
        assertEquals(0, state.shards());
        assertEquals(List.of(70, 22, 7, 1), state.openPercent());
    }

    // ---------------------------------------------------------------- catalog shape

    @Test
    void theCachePoolHasEveryCategoryAndRarityExactlyOnce() {
        List<CosmeticCatalog.Item> pool = CosmeticCatalog.bySource(CosmeticSource.CACHE);
        assertEquals(12, pool.size());                     // 3 categories x 4 rarities
        for (CosmeticRarity rarity : CosmeticRarity.values()) {
            assertEquals(3, CosmeticCatalog.cachePool(rarity).size(), rarity.name());
            for (CosmeticCatalog.Item item : CosmeticCatalog.cachePool(rarity)) {
                assertTrue(CosmeticCatalog.isCacheCategory(item.category()));
            }
        }
        assertEquals(1, CosmeticCatalog.bySource(CosmeticSource.SPECIAL).size());
        assertEquals("aura.ascended", CosmeticCatalog.bySource(CosmeticSource.SPECIAL).get(0).key());
        assertEquals(CosmeticCategory.AURA, CosmeticCatalog.find("aura.ascended").orElseThrow().category());
    }
}
