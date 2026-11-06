package com.problemsolving.platform.api.dto.cosmetics;

/** What one opened Mystery Cache contained. */
public record CacheOpenResultDto(
        String rarity,
        /** The cosmetic the cache rolled (named even when it was a duplicate). */
        String key,
        String name,
        String category,
        /** True when the cosmetic was already owned: the cache turned into shards instead. */
        boolean wasDuplicate,
        /** Shards granted on top (non-zero only for duplicates). */
        long shardsGained,
        long shards,
        int unopenedCaches) {
}
