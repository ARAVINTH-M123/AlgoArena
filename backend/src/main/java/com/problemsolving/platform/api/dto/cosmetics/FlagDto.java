package com.problemsolving.platform.api.dto.cosmetics;

import java.util.List;

/**
 * A nation flag: one pattern, two or three colors and an optional emblem.
 * All values are cosmetic catalog keys ("pattern.cross", "color.slate", "emblem.gear");
 * the look lives in the frontend (lib/flags.ts). emblem is null when the owner chose no emblem.
 */
public record FlagDto(String pattern, List<String> colors, String emblem) {
}
