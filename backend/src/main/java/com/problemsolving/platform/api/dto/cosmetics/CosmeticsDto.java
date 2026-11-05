package com.problemsolving.platform.api.dto.cosmetics;

import java.util.List;

/**
 * The signed-in user's cosmetics. themeKey / borderKey are the ones in effect. pinnedThemeKey / pinnedBorderKey
 * are what the owner chose explicitly; null means "automatic": follow the cosmetic of the current rank.
 */
public record CosmeticsDto(
        String themeKey,
        String borderKey,
        String pinnedThemeKey,
        String pinnedBorderKey,
        List<CosmeticItemDto> items) {
}
