// Nation flags (Phase 5B): the look of every flag pattern, color and emblem.
// All keys match the backend catalog (CosmeticCatalog); the shape data lives here only.

import type { FlagDto } from "@/lib/cosmetics";
import { RANK_NAMES } from "@/lib/nation";

// ------------------------------------------------------------------ colors (blueprint 9.4)

export const FLAG_COLOR_HEX: Record<string, string> = {
  "color.slate": "#3a4655",
  "color.charcoal": "#1f2937",
  "color.white": "#f3f4f6",
  "color.crimson": "#dc2626",
  "color.orange": "#f97316",
  "color.amber": "#fbbf24",
  "color.green": "#22c55e",
  "color.teal": "#14b8a6",
  "color.sky": "#38bdf8",
  "color.blue": "#2563eb",
  "color.violet": "#8b5cf6",
  "color.pink": "#ec4899"
};

export function flagColorHex(key: string): string {
  return FLAG_COLOR_HEX[key] ?? "#3a4655";
}

export const FLAG_COLOR_KEYS = Object.keys(FLAG_COLOR_HEX);

// ------------------------------------------------------------------ patterns

export type FlagPattern = {
  /** Human name for the picker. */
  label: string;
  /** True when the pattern really needs a third color to look right (picker hint). */
  wantsThirdColor: boolean;
};

export const FLAG_PATTERNS: Record<string, FlagPattern> = {
  "pattern.horizontal": { label: "Horizontal", wantsThirdColor: true },
  "pattern.vertical": { label: "Vertical", wantsThirdColor: true },
  "pattern.diagonal": { label: "Diagonal", wantsThirdColor: true },
  "pattern.cross": { label: "Nordic Cross", wantsThirdColor: true },
  "pattern.saltire": { label: "Saltire", wantsThirdColor: true },
  "pattern.triangle": { label: "Hoist Triangle", wantsThirdColor: true },
  "pattern.canton": { label: "Canton", wantsThirdColor: true }
};

export const FLAG_PATTERN_KEYS = Object.keys(FLAG_PATTERNS);

// ------------------------------------------------------------------ emblems (24x24 glyphs)

/** One emblem: a simple 24x24 SVG path drawn in one color, plus the rank it belongs to (index into RANK_NAMES). */
export const EMBLEMS_BY_RANK: { key: string; label: string }[] = [
  { key: "emblem.cursor", label: "Cursor" },       // Null
  { key: "emblem.seedling", label: "Seedling" },   // Init
  { key: "emblem.gear", label: "Gear" },           // Boot
  { key: "emblem.roots", label: "Roots" },         // Root
  { key: "emblem.shield", label: "Shield" },       // Kernel
  { key: "emblem.stack", label: "Stack" },         // Stack
  { key: "emblem.cloud", label: "Cloud" },         // Cloud
  { key: "emblem.core", label: "Core" },           // Core
  { key: "emblem.atom", label: "Atom" },           // Quantum
  { key: "emblem.spiral", label: "Spiral" }        // Singularity
];

/** d attribute of a 24x24 path (or a list of sub-paths) per emblem key. One fill color, no stroke. */
export const EMBLEM_GLYPHS: Record<string, string[]> = {
  // a blinking-terminal cursor block
  "emblem.cursor": ["M4 5 h16 v14 h-16 Z M7 9 h10 v2 h-10 Z"],
  // a sprout: stem + two leaves
  "emblem.seedling": [
    "M11 21 v-7 c0 -3 1.5 -5 5 -6 c-0.5 3.5 -2 5.5 -5 6 Z",
    "M11 21 v-9 c0 -3 -1.5 -5 -5 -6 c0.5 3.5 2 5.5 5 6 Z"
  ],
  // a gear: ring with teeth (approximated as a thick ring + 4 studs)
  "emblem.gear": [
    "M10.5 3 h3 v3.2 a6 6 0 0 0 -3 0 Z M10.5 17.8 a6 6 0 0 0 3 0 V21 h-3 Z",
    "M3 10.5 h3.2 a6 6 0 0 0 0 3 H3 Z M17.8 10.5 a6 6 0 0 1 0 3 H21 v-3 Z",
    "M12 8 a4 4 0 1 1 0 8 a4 4 0 0 1 0 -8 Z M12 10.6 a1.4 1.4 0 1 0 0 2.8 a1.4 1.4 0 0 0 0 -2.8 Z"
  ],
  // roots: trunk fanning into three roots
  "emblem.roots": ["M11 3 h2 v7 l4 5 -1.6 1.2 -2.4 -3 V21 h-2 v-7.8 l-2.4 3 -1.6 -1.2 4 -5 Z"],
  // a heater shield
  "emblem.shield": ["M12 2 L20 5 V12 C20 17 16.5 20.5 12 22 C7.5 20.5 4 17 4 12 V5 Z M12 4.4 L6 6.6 V12 C6 15.9 8.6 18.7 12 20.2 C15.4 18.7 18 15.9 18 12 V6.6 Z"],
  // three stacked plates
  "emblem.stack": [
    "M12 3 L21 7 L12 11 L3 7 Z",
    "M5.2 10.4 L3 11.4 L12 15.4 L21 11.4 L18.8 10.4 L12 13.5 Z",
    "M5.2 14.8 L3 15.8 L12 19.8 L21 15.8 L18.8 14.8 L12 17.9 Z"
  ],
  // a cloud
  "emblem.cloud": ["M7 18 a4 4 0 0 1 -0.6 -7.95 A5.5 5.5 0 0 1 17 8.6 A4.2 4.2 0 0 1 16.8 18 Z"],
  // a core: hexagon with a dot
  "emblem.core": ["M12 2 L20.66 7 V17 L12 22 L3.34 17 V7 Z M12 4.3 L5.34 8.2 V15.8 L12 19.7 L18.66 15.8 V8.2 Z M12 9.5 a2.5 2.5 0 1 1 0 5 a2.5 2.5 0 0 1 0 -5 Z"],
  // an atom: nucleus + two electron rings (as ellipse-ish paths)
  "emblem.atom": [
    "M12 10.6 a1.6 1.6 0 1 1 0 3.2 a1.6 1.6 0 0 1 0 -3.2 Z",
    "M6.7 6.2 C9 3.9 12.4 3 15 4.4 c2.6 1.4 3.3 4.8 2.3 8 1 3.2 0.3 6.6 -2.3 8 c-2.6 1.4 -6 0.5 -8.3 -1.8 C4.4 16.3 3.7 12.9 4.7 9.7 c-1 -3.2 -0.3 -2.1 2 -3.5 Z M6.9 8.5 C5.4 11 5.6 14.4 7.9 16.7 c2.3 2.3 5.7 2.5 8.2 1 -1.5 -2.5 -1.3 -5.9 1 -8.2 -2.5 1.5 -5.9 1.3 -8.2 -1 Z"
  ],
  // a spiral (Archimedean, drawn as one path)
  "emblem.spiral": ["M12 12 c0 -1.2 1.8 -1.2 1.8 0 c0 1.9 -2.4 2.6 -3.9 1.4 c-2 -1.6 -1.2 -4.8 1.1 -5.8 c3 -1.3 6.3 0.6 6.9 3.7 c0.7 3.9 -2.4 7.4 -6.3 7.6 c-4.8 0.2 -8.6 -4 -8 -8.8 C4.3 4.6 9.5 1.2 14.6 2.4"]
};

export function emblemGlyph(key: string | null | undefined): string[] | null {
  return (key && EMBLEM_GLYPHS[key]) || null;
}

/** The emblem key of a rank (index into RANK_NAMES). */
export function emblemForRank(rankIndex: number): string {
  return EMBLEMS_BY_RANK[Math.min(Math.max(rankIndex, 0), EMBLEMS_BY_RANK.length - 1)].key;
}

// ------------------------------------------------------------------ defaults

export const DEFAULT_FLAG_PATTERN = "pattern.horizontal";
export const DEFAULT_FLAG_COLORS = ["color.slate", "color.charcoal"];

/** The flag a nation shows until the owner picks their own (mirrors CosmeticService.defaultFlag). */
export function defaultFlag(rankIndex: number): FlagDto {
  return {
    pattern: DEFAULT_FLAG_PATTERN,
    colors: [...DEFAULT_FLAG_COLORS],
    emblem: emblemForRank(rankIndex)
  };
}

/** Never trusts a partial payload from the network. */
export function normalizeFlag(flag: FlagDto | null | undefined, rankIndex: number): FlagDto {
  if (!flag || !FLAG_PATTERNS[flag.pattern] || !Array.isArray(flag.colors)) {
    return defaultFlag(rankIndex);
  }
  const colors = flag.colors.filter((color) => FLAG_COLOR_HEX[color] !== undefined);
  if (colors.length < 2 || colors.length > 3) {
    return defaultFlag(rankIndex);
  }
  const emblem = flag.emblem && EMBLEM_GLYPHS[flag.emblem] ? flag.emblem : null;
  return { pattern: flag.pattern, colors, emblem };
}
