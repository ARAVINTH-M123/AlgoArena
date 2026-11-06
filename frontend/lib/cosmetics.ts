// Cosmetics (visual only): types and the look of every theme, frame, anthem and scene cosmetic.
// Keys match the backend catalog.

export type EquippableCategory = "THEME" | "BORDER" | "ANTHEM" | "SKY" | "AURA" | "AMBIENT";

export type CosmeticItemDto = {
  key: string;
  category: string;
  rarity: "COMMON" | "RARE" | "EPIC" | "LEGENDARY";
  displayName: string;
  unlockRank: number | null;
  unlockRankName: string | null;
  owned: boolean;
  /** In use right now (chosen by the owner, or automatic for the current rank). */
  equipped: boolean;
};

export type CosmeticsDto = {
  themeKey: string;
  borderKey: string;
  /** What the owner chose; null = automatic (follows the current rank). */
  pinnedThemeKey: string | null;
  pinnedBorderKey: string | null;
  anthemKey: string;
  pinnedAnthemKey: string | null;
  /** Scene cosmetics (Phase 6): null = none (the rank's plain look). They come from caches or crafting. */
  skyKey: string | null;
  pinnedSkyKey: string | null;
  auraKey: string | null;
  pinnedAuraKey: string | null;
  ambientKey: string | null;
  pinnedAmbientKey: string | null;
  /** The nation's flag in effect (never null: the default flag of the current rank). */
  flag: FlagDto;
  items: CosmeticItemDto[];
};

/** A flag: cosmetic catalog keys. emblem is null when the owner chose no emblem. */
export type FlagDto = {
  pattern: string;
  colors: string[];
  emblem: string | null;
};

// ------------------------------------------------------------------ editor themes (blueprint 9.1)

export type ThemeLook = { background: string; accent: string; mood: string };

export const DEFAULT_THEME_KEY = "theme.void";
export const DEFAULT_BORDER_KEY = "border.thin-gray";

export const THEME_LOOKS: Record<string, ThemeLook> = {
  "theme.void": { background: "#05070a", accent: "#3a4655", mood: "near-black, muted gray" },
  "theme.ember": { background: "#0c0806", accent: "#ff7a2f", mood: "warm firelight" },
  "theme.terminal": { background: "#020a05", accent: "#39ff88", mood: "classic green terminal" },
  "theme.timber": { background: "#120d08", accent: "#d9a066", mood: "warm browns" },
  "theme.stonework": { background: "#0e1013", accent: "#8fb3c9", mood: "cool slate" },
  "theme.brickline": { background: "#130a0a", accent: "#e0624d", mood: "brick red" },
  "theme.skyline": { background: "#0a1018", accent: "#5ab0ff", mood: "sky blue" },
  "theme.circuit": { background: "#06100f", accent: "#19e6c1", mood: "teal circuit traces" },
  "theme.aurora": { background: "#0a0716", accent: "#b07cff", mood: "violet aurora" },
  "theme.event-horizon": { background: "#000000", accent: "#ffd45a", mood: "black with a subtle animated gold glow" }
};

export function themeLook(key: string | null | undefined): ThemeLook {
  return THEME_LOOKS[key ?? ""] ?? THEME_LOOKS[DEFAULT_THEME_KEY];
}

// ------------------------------------------------------------------ nation frames (blueprint 9.3)

/**
 * A frame is drawn as a padded wrapper: the padding area IS the border, so gradients, patterns and animation
 * work with rounded corners (plain CSS borders cannot do that).
 */
export type BorderStyle = {
  padding: number;
  background: string;
  radius: number;
  glow?: string;
  animated?: boolean;
};

export const BORDER_STYLES: Record<string, BorderStyle> = {
  "border.thin-gray": { padding: 1, background: "#3a4655", radius: 12 },
  "border.ember-glow": { padding: 2, background: "#ff7a2f", radius: 12, glow: "0 0 16px rgba(255,122,47,0.55)" },
  "border.green-pixel": {
    padding: 5,
    // checkerboard: reads as chunky pixels on all four sides
    background: "linear-gradient(45deg, #0b3d1f 25%, transparent 25%, transparent 75%, #0b3d1f 75%) 0 0 / 10px 10px, linear-gradient(45deg, #0b3d1f 25%, transparent 25%, transparent 75%, #0b3d1f 75%) 5px 5px / 10px 10px, #39ff88",
    radius: 0
  },
  "border.wood-grain": { padding: 7, background: "repeating-linear-gradient(95deg, #d9a066 0px, #d9a066 6px, #a8742f 6px, #a8742f 9px, #c48a46 9px, #c48a46 16px)", radius: 10 },
  "border.stone": { padding: 7, background: "repeating-linear-gradient(45deg, #5c6b78 0px, #5c6b78 7px, #8fb3c9 7px, #8fb3c9 13px)", radius: 6 },
  "border.riveted-metal": {
    padding: 7,
    background: "radial-gradient(circle, #e5e8ec 1.4px, transparent 2px) 0 0 / 14px 14px, linear-gradient(135deg, #9aa0a8, #5b6068 45%, #b7bcc3 60%, #6e747c)",
    radius: 6
  },
  "border.white-mist": { padding: 3, background: "linear-gradient(180deg, rgba(255,255,255,0.75), rgba(255,255,255,0.25))", radius: 14, glow: "0 0 26px rgba(255,255,255,0.35)" },
  "border.circuit-traces": { padding: 5, background: "repeating-linear-gradient(90deg, #19e6c1 0px, #19e6c1 14px, #06302b 14px, #06302b 18px)", radius: 8, glow: "0 0 14px rgba(25,230,193,0.4)" },
  "border.shimmer": { padding: 4, background: "linear-gradient(120deg, #b07cff, #5ab0ff, #ff7ac8, #b07cff, #5ab0ff)", radius: 14, glow: "0 0 18px rgba(176,124,255,0.4)", animated: true },
  "border.prismatic-gold": { padding: 5, background: "linear-gradient(120deg, #ffd45a, #fff3b0, #ff9d5a, #ffe27a, #ffd45a)", radius: 14, glow: "0 0 24px rgba(255,212,90,0.5)", animated: true }
};

export function borderStyleFor(key: string | null | undefined): BorderStyle {
  return BORDER_STYLES[key ?? ""] ?? BORDER_STYLES[DEFAULT_BORDER_KEY];
}

export const CATEGORY_LABELS: Record<string, string> = {
  THEME: "editor theme",
  BORDER: "nation frame",
  ANTHEM: "anthem",
  FLAG_PATTERN: "flag pattern",
  FLAG_COLOR: "flag color",
  FLAG_EMBLEM: "flag emblem",
  SKY: "sky",
  AURA: "aura",
  AMBIENT: "ambient"
};

// ------------------------------------------------------------------ anthem titles (keys match the backend)

export const ANTHEM_TITLES: Record<string, string> = {
  "anthem.soft-pulse": "Soft Pulse",
  "anthem.campfire": "Campfire",
  "anthem.chiptune": "Chiptune",
  "anthem.folk": "Folk Hymn",
  "anthem.march": "Slow March",
  "anthem.industrial": "Industrial Heart",
  "anthem.airy": "Airy Skies",
  "anthem.synth": "Synth Dawn",
  "anthem.shimmer": "Shimmer",
  "anthem.ascension": "Ascension"
};

export function anthemTitle(key: string | null | undefined): string {
  return ANTHEM_TITLES[key ?? ""] ?? "Anthems";
}

// ------------------------------------------------------------------ scene cosmetics (Phase 6)

/** A sky replaces the rank's sky gradient (top -> bottom). */
export type SkyLook = { top: string; bottom: string };

export const SKY_LOOKS: Record<string, SkyLook> = {
  "sky.meadow-dawn": { top: "#12233a", bottom: "#3a5a2f" },
  "sky.dusk-fire": { top: "#2a0f0a", bottom: "#7a2d12" },
  "sky.nebula": { top: "#120b2e", bottom: "#5a2a8a" },
  "sky.void-veil": { top: "#000000", bottom: "#101426" }
};

export function skyLook(key: string | null | undefined): SkyLook | null {
  return SKY_LOOKS[key ?? ""] ?? null;
}

/** An aura is a glowing ring around the presence figure. */
export type AuraLook = { color: string; /** ring width in scene units */ width: number; animated: boolean };

export const AURA_LOOKS: Record<string, AuraLook> = {
  "aura.ember-ring": { color: "#ff7a2f", width: 2.2, animated: false },
  "aura.tidal-glow": { color: "#38bdf8", width: 2.8, animated: true },
  "aura.aurora-crown": { color: "#b07cff", width: 3.4, animated: true },
  "aura.star-shard": { color: "#ffd45a", width: 4, animated: true },
  "aura.ascended": { color: "#ffe9a3", width: 4.6, animated: true }
};

export function auraLook(key: string | null | undefined): AuraLook | null {
  return AURA_LOOKS[key ?? ""] ?? null;
}

/** An ambient effect scatters little motes over the scene (deterministic positions). */
export type AmbientLook = { color: string; count: number; rise: boolean };

export const AMBIENT_LOOKS: Record<string, AmbientLook> = {
  "ambient.pollen": { color: "#d9a066", count: 10, rise: false },
  "ambient.ember-motes": { color: "#ff7a2f", count: 12, rise: true },
  "ambient.fireflies": { color: "#a3e635", count: 14, rise: false },
  "ambient.stardust": { color: "#e0d7ff", count: 18, rise: true }
};

export function ambientLook(key: string | null | undefined): AmbientLook | null {
  return AMBIENT_LOOKS[key ?? ""] ?? null;
}

// ------------------------------------------------------------------ Mystery Caches (Phase 6)

export const RARITY_ORDER = ["COMMON", "RARE", "EPIC", "LEGENDARY"] as const;
export type Rarity = (typeof RARITY_ORDER)[number];

export function rarityColor(rarity: string): string {
  switch (rarity) {
    case "RARE": return "#38bdf8";
    case "EPIC": return "#b07cff";
    case "LEGENDARY": return "#ffd45a";
    default: return "#9ca3af";
  }
}

/** GET /api/cosmetics/caches: counters plus the exact odds, shown in the UI with no secrets. */
export type CacheStateDto = {
  unopenedCaches: number;
  shards: number;
  solvesSinceCache: number;
  solvesForGuarantee: number;
  dropPercent: number[];      // EASY, MEDIUM, HARD
  opensSinceRare: number;
  opensForRarePity: number;
  openPercent: number[];      // COMMON, RARE, EPIC, LEGENDARY
  duplicateShards: number[];
  craftCost: number[];
};

/** POST /api/cosmetics/caches/open: what one cache contained. */
export type CacheOpenResultDto = {
  rarity: Rarity;
  key: string;
  name: string;
  category: string;
  wasDuplicate: boolean;
  shardsGained: number;
  shards: number;
  unopenedCaches: number;
};

/** POST /api/cosmetics/craft. */
export type CraftResultDto = {
  key: string;
  name: string;
  category: string;
  rarity: Rarity;
  shardsSpent: number;
  shardsLeft: number;
};
