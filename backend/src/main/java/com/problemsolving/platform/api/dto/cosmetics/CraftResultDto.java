package com.problemsolving.platform.api.dto.cosmetics;

/** The result of crafting one cosmetic with shards. */
public record CraftResultDto(
        String key,
        String name,
        String category,
        String rarity,
        long shardsSpent,
        long shardsLeft) {
}
