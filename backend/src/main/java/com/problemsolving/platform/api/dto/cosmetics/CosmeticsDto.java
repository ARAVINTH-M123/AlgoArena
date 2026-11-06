package com.problemsolving.platform.api.dto.cosmetics;

import java.util.List;

/**
 * The signed-in user's cosmetics. themeKey / borderKey / anthemKey are the ones in effect. pinnedThemeKey /
 * pinnedBorderKey / pinnedAnthemKey are what the owner chose explicitly; null means "automatic": follow the
 * cosmetic of the current rank. skyKey / auraKey / ambientKey (Phase 6) come from caches or crafting and have
 * no rank default: null means "none". flag is the nation's flag in effect (never null; the default is computed).
 */
public record CosmeticsDto(
        String themeKey,
        String borderKey,
        String pinnedThemeKey,
        String pinnedBorderKey,
        String anthemKey,
        String pinnedAnthemKey,
        String skyKey,
        String pinnedSkyKey,
        String auraKey,
        String pinnedAuraKey,
        String ambientKey,
        String pinnedAmbientKey,
        FlagDto flag,
        List<CosmeticItemDto> items) {
}
