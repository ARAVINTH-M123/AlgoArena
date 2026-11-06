package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.api.dto.cosmetics.CacheOpenResultDto;
import com.problemsolving.platform.api.dto.cosmetics.CacheStateDto;
import com.problemsolving.platform.api.dto.cosmetics.CraftResultDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.CacheOpenLog;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationCosmeticOwned;
import com.problemsolving.platform.domain.enums.CosmeticCategory;
import com.problemsolving.platform.domain.enums.CosmeticRarity;
import com.problemsolving.platform.domain.enums.CosmeticSource;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.service.nation.NationStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * Mystery Caches (Phase 6). An accepted FIRST solve may drop a cache (chance by difficulty, with a 40-solve
 * pity). Opening one rolls a rarity (70/22/7/1, with an every-10th Rare-or-better pity) and grants a scene
 * cosmetic from the cache pool; a duplicate turns into shards, which crafting spends.
 *
 * All randomness comes from one injected {@link Random} so tests can script it deterministically.
 * Every number in here is fixed in code (no yaml overrides), like the rest of the gameplay tables.
 */
@Service
public class CacheService {

    /** First-solves without a drop after which the next first-solve is guaranteed to drop one. */
    public static final int PITY_SOLVES = 40;
    /** Opens without a Rare-or-better after which the next open is forced Rare or better. */
    public static final int RARE_PITY_OPENS = 10;

    /** Drop chance in percent per accepted FIRST solve, index = Difficulty ordinal (EASY, MEDIUM, HARD). */
    static final int[] DROP_PERCENT = {3, 6, 12};
    /** Open odds in percent, index = CosmeticRarity ordinal (COMMON, RARE, EPIC, LEGENDARY). */
    static final int[] OPEN_PERCENT = {70, 22, 7, 1};
    /** Shards for a duplicate, index = CosmeticRarity ordinal (a quarter of the craft cost). */
    static final long[] DUPLICATE_SHARDS = {10, 30, 100, 300};
    /** Craft cost per rarity, index = CosmeticRarity ordinal. */
    static final long[] CRAFT_COST = {40, 120, 400, 1200};

    private final NationStore store;
    private final Random random;

    @Autowired
    public CacheService(NationStore store) {
        this(store, new Random());
    }

    /** Test constructor: script the randomness. */
    public CacheService(NationStore store, Random random) {
        this.store = store;
        this.random = random;
    }

    // ------------------------------------------------------------------ drop (called from the award path)

    /**
     * Rolls whether this accepted FIRST solve drops a Mystery Cache. Mutates the nation's counters
     * (the caller saves the nation inside its own transaction). Re-solves never reach this method.
     */
    public boolean rollDrop(Nation nation, Difficulty difficulty, Instant now) {
        nation.setSolvesSinceCache(nation.getSolvesSinceCache() + 1);
        boolean pity = nation.getSolvesSinceCache() >= PITY_SOLVES;
        boolean lucky = random.nextDouble() * 100 < DROP_PERCENT[difficulty.ordinal()];
        if (pity || lucky) {
            nation.setUnopenedCaches(nation.getUnopenedCaches() + 1);
            nation.setSolvesSinceCache(0);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ open

    @Transactional
    public CacheOpenResultDto open(String username, Instant now) {
        Long userId = resolveUserId(username);
        Nation nation = store.lockOrCreateNation(userId);
        if (nation.getUnopenedCaches() <= 0) {
            throw bad("No Mystery Cache to open - solve problems to earn one");
        }

        CosmeticRarity rarity = rollRarity(nation);
        CosmeticCatalog.Item item = pickItem(userId, rarity);

        boolean duplicate = store.ownsCosmetic(userId, item.key());
        long shardsGained = 0;
        if (duplicate) {
            shardsGained = DUPLICATE_SHARDS[rarity.ordinal()];
            nation.setShards(nation.getShards() + shardsGained);
        } else {
            grant(userId, item, CosmeticSource.CACHE, now);
        }

        nation.setUnopenedCaches(nation.getUnopenedCaches() - 1);
        nation.setUpdatedAt(now);
        store.saveNation(nation);

        CacheOpenLog log = new CacheOpenLog();
        log.setUserId(userId);
        log.setOpenedAt(now);
        log.setRarity(rarity.name());
        log.setCosmeticKey(item.key());
        log.setShardsGained(shardsGained);
        log.setWasDuplicate(duplicate);
        store.saveCacheOpenLog(log);

        return new CacheOpenResultDto(rarity.name(), item.key(), item.displayName(),
                item.category().name(), duplicate, shardsGained, nation.getShards(), nation.getUnopenedCaches());
    }

    /** The rarity of this open, honoring the every-10th Rare-or-better pity. Also updates the pity counter. */
    private CosmeticRarity rollRarity(Nation nation) {
        boolean forced = nation.getOpensSinceRare() + 1 >= RARE_PITY_OPENS;
        CosmeticRarity rarity;
        if (forced) {
            // renormalized weights inside the Rare-or-better space: 0..21 RARE, 22..28 EPIC, 29 LEGENDARY
            int roll = random.nextInt(OPEN_PERCENT[1] + OPEN_PERCENT[2] + OPEN_PERCENT[3]);
            rarity = roll < OPEN_PERCENT[1] ? CosmeticRarity.RARE
                    : roll < OPEN_PERCENT[1] + OPEN_PERCENT[2] ? CosmeticRarity.EPIC
                    : CosmeticRarity.LEGENDARY;
        } else {
            rarity = rarityFromRoll((int) (random.nextDouble() * 100), OPEN_PERCENT[0], OPEN_PERCENT[1], OPEN_PERCENT[2]);
        }
        nation.setOpensSinceRare(rarity.ordinal() >= CosmeticRarity.RARE.ordinal() ? 0 : nation.getOpensSinceRare() + 1);
        return rarity;
    }

    /** Walks cumulative weights; the last rarity wins any roll beyond 99. */
    static CosmeticRarity rarityFromRoll(int roll, int commonPercent, int rarePercent, int epicPercent) {
        int r = Math.max(0, Math.min(roll, 99));
        if (r < commonPercent) {
            return CosmeticRarity.COMMON;
        }
        if (r < commonPercent + rarePercent) {
            return CosmeticRarity.RARE;
        }
        if (r < commonPercent + rarePercent + epicPercent) {
            return CosmeticRarity.EPIC;
        }
        return CosmeticRarity.LEGENDARY;
    }

    /** A cache-pool cosmetic of the rolled rarity: unowned ones first, so collections fill up. */
    private CosmeticCatalog.Item pickItem(Long userId, CosmeticRarity rarity) {
        List<CosmeticCatalog.Item> pool = CosmeticCatalog.cachePool(rarity);
        List<CosmeticCatalog.Item> unowned = pool.stream()
                .filter(item -> !store.ownsCosmetic(userId, item.key())).toList();
        List<CosmeticCatalog.Item> choices = unowned.isEmpty() ? pool : unowned;
        return choices.get(random.nextInt(choices.size()));
    }

    // ------------------------------------------------------------------ craft

    @Transactional
    public CraftResultDto craft(String username, String key, Instant now) {
        Long userId = resolveUserId(username);
        CosmeticCatalog.Item item = CosmeticCatalog.find(key == null ? "" : key.trim())
                .orElseThrow(() -> bad("Unknown cosmetic"));
        if (item.source() != CosmeticSource.CACHE) {
            throw bad("Only cache cosmetics can be crafted");
        }
        if (store.ownsCosmetic(userId, item.key())) {
            throw bad("You already own " + item.displayName());
        }

        Nation nation = store.lockOrCreateNation(userId);
        long cost = CRAFT_COST[item.rarity().ordinal()];
        if (nation.getShards() < cost) {
            throw bad("Not enough shards: crafting " + item.displayName() + " needs " + cost
                    + ", you have " + nation.getShards());
        }

        nation.setShards(nation.getShards() - cost);
        nation.setUpdatedAt(now);
        store.saveNation(nation);
        grant(userId, item, CosmeticSource.SHARDS, now);

        return new CraftResultDto(item.key(), item.displayName(), item.category().name(),
                item.rarity().name(), cost, nation.getShards());
    }

    // ------------------------------------------------------------------ special grants

    /** Grants the Ascended Aura once, when the Teleport Gate completes at Singularity. Idempotent. */
    public void grantAscendedAura(Long userId, Instant now) {
        if (store.ownsCosmetic(userId, "aura.ascended")) {
            return;
        }
        grant(userId, CosmeticCatalog.find("aura.ascended").orElseThrow(), CosmeticSource.SPECIAL, now);
    }

    private void grant(Long userId, CosmeticCatalog.Item item, CosmeticSource via, Instant now) {
        NationCosmeticOwned owned = new NationCosmeticOwned();
        owned.setUserId(userId);
        owned.setCosmeticKey(item.key());
        owned.setAcquiredVia(via.name());
        owned.setAcquiredAt(now);
        store.saveOwnedCosmetic(owned);
    }

    // ------------------------------------------------------------------ state for the UI

    @Transactional(readOnly = true)
    public CacheStateDto state(String username) {
        Long userId = resolveUserId(username);
        Nation nation = store.findNation(userId).orElse(null);
        return stateOf(nation);
    }

    /** Counters and odds; safe for a user without a nation (everything zero). */
    public static CacheStateDto stateOf(Nation nation) {
        return new CacheStateDto(
                nation == null ? 0 : nation.getUnopenedCaches(),
                nation == null ? 0 : nation.getShards(),
                nation == null ? 0 : nation.getSolvesSinceCache(),
                PITY_SOLVES,
                List.of(DROP_PERCENT[0], DROP_PERCENT[1], DROP_PERCENT[2]),
                nation == null ? 0 : nation.getOpensSinceRare(),
                RARE_PITY_OPENS,
                List.of(OPEN_PERCENT[0], OPEN_PERCENT[1], OPEN_PERCENT[2], OPEN_PERCENT[3]),
                List.of(DUPLICATE_SHARDS[0], DUPLICATE_SHARDS[1], DUPLICATE_SHARDS[2], DUPLICATE_SHARDS[3]),
                List.of(CRAFT_COST[0], CRAFT_COST[1], CRAFT_COST[2], CRAFT_COST[3]));
    }

    // ------------------------------------------------------------------ helpers

    private Long resolveUserId(String username) {
        return store.findUserId(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
