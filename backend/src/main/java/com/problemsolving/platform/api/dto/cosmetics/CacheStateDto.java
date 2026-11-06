package com.problemsolving.platform.api.dto.cosmetics;

import java.util.List;

/** The signed-in user's cache and shard state, plus the exact odds (shown in the UI, no secrets). */
public record CacheStateDto(
        int unopenedCaches,
        long shards,
        int solvesSinceCache,
        /** A cache is guaranteed on this many first-solves without a drop. */
        int solvesForGuarantee,
        /** Drop chance in percent per difficulty: EASY / MEDIUM / HARD. */
        List<Integer> dropPercent,
        int opensSinceRare,
        /** Every open with this number without a Rare-or-better is forced to be Rare or better. */
        int opensForRarePity,
        /** Open odds in percent, rarity order COMMON, RARE, EPIC, LEGENDARY. */
        List<Integer> openPercent,
        /** Shards granted when a cache contains a duplicate, same order. */
        List<Long> duplicateShards,
        /** Shards needed to craft a cosmetic of each rarity, same order. */
        List<Long> craftCost) {
}
