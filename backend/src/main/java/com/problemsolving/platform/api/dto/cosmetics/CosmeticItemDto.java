package com.problemsolving.platform.api.dto.cosmetics;

/** One cosmetic as shown in the catalog / picker. */
public record CosmeticItemDto(
        String key,
        String category,
        String rarity,
        String displayName,
        Integer unlockRank,
        String unlockRankName,
        boolean owned,
        /** True when this is the cosmetic currently in use (pinned by the owner or automatic for the rank). */
        boolean equipped) {
}
