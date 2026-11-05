// Nation system: shared types, formatting helpers, rank palettes and the deterministic city layout.
// Point values coming from the API are micro-points (1 point = 1,000,000); the UI shows whole points.

import type { PresenceDto } from "@/lib/presence";

export type TrackName = "BASICS" | "STRUCTURES" | "LANDMARKS";

export type TrackProgressDto = {
  track: TrackName;
  itemKey: string;
  itemName: string;
  progress: number;
  cost: number;
  stage: number;
};

export type RequirementDto = {
  type: "XP" | "MEDIUMS" | "HARDS" | "ACTIVE_DAYS" | "GATE_LANDMARK";
  label: string;
  current: number;
  required: number;
  met: boolean;
};

export type NextRankDto = {
  name: string;
  xpRequired: number;
  percentToNext: number;
  requirements: RequirementDto[];
};

export type CompletedItemDto = {
  track: TrackName;
  key: string;
  name: string;
  era: number;
  sequenceNumber: number;
  instance: number;
};

export type NationDto = {
  username: string;
  name: string;
  owner: boolean;
  rankIndex: number;
  rankName: string;
  tier: number;
  ascended: boolean;
  xp: number;
  nextRank: NextRankDto | null;
  solves: { easy: number; medium: number; hard: number; activeDays: number };
  tracks: TrackProgressDto[];
  completedItems: CompletedItemDto[];
  timeZone: string;
  renameAvailableAt: string | null;
  timeZoneChangeAvailableAt: string | null;
  presence: PresenceDto;
  /** True only for the owner while "Appear offline" is on (visitors always get false). */
  appearOffline: boolean;
  /** Editor theme and frame in effect for this nation (see lib/cosmetics.ts). */
  themeKey: string;
  borderKey: string;
};

export type UpdateNationProfilePayload = {
  name?: string;
  timeZone?: string;
  appearOffline?: boolean;
};

// ------------------------------------------------------------------ formatting

export const MICRO = 1_000_000;

export const RANK_NAMES = ["Null", "Init", "Boot", "Root", "Kernel", "Stack", "Cloud", "Core", "Quantum", "Singularity"];

export function wholePoints(micro: number): number {
  return Math.floor(micro / MICRO);
}

export function formatPoints(micro: number): string {
  return wholePoints(micro).toLocaleString("en-US");
}

/**
 * Points gained by one solve. Whole points normally; tiny re-solve bonuses and halved amounts keep their
 * decimals (0.5, 0.001) so the player can see something real happened.
 */
export function formatGain(micro: number): string {
  if (micro <= 0) return "0";
  if (micro >= MICRO) return Math.floor(micro / MICRO).toLocaleString("en-US");
  const text = (micro / MICRO).toFixed(3).replace(/0+$/, "").replace(/\.$/, "");
  return text === "0" ? "<0.001" : text;
}

const TIER_LABELS = ["I", "II", "III"];

export function tierLabel(tier: number): string {
  return TIER_LABELS[Math.min(Math.max(tier, 1), 3) - 1];
}

export function rankTitle(rankName: string, tier: number): string {
  return `${rankName} ${tierLabel(tier)}`;
}

export const STAGE_LABELS = ["Foundation", "Frame", "Walls", "Roof"];

export const TRACK_LABELS: Record<TrackName, string> = {
  BASICS: "Basics",
  STRUCTURES: "Structures",
  LANDMARKS: "Landmarks"
};

// ------------------------------------------------------------------ rank looks

export type RankPalette = {
  skyTop: string;
  skyBottom: string;
  mountainBack: string;
  mountainFront: string;
  ground: string;
  accent: string;
  stars: number;
  ambient: "cursor" | "fire" | "glow" | "none";
};

// Index = rank index (Null .. Singularity). Colors follow the editor-theme table of the blueprint.
export const RANK_PALETTES: RankPalette[] = [
  { skyTop: "#05070a", skyBottom: "#0b1016", mountainBack: "#11161d", mountainFront: "#161c25", ground: "#0a0d11", accent: "#3a4655", stars: 0, ambient: "cursor" },
  { skyTop: "#0c0806", skyBottom: "#2a1208", mountainBack: "#1a0f09", mountainFront: "#26140c", ground: "#120a06", accent: "#ff7a2f", stars: 0, ambient: "fire" },
  { skyTop: "#020a05", skyBottom: "#052a14", mountainBack: "#06170d", mountainFront: "#0a2415", ground: "#04100a", accent: "#39ff88", stars: 0, ambient: "none" },
  { skyTop: "#120d08", skyBottom: "#3a2512", mountainBack: "#2a1d10", mountainFront: "#3a2814", ground: "#1a120a", accent: "#d9a066", stars: 0, ambient: "none" },
  { skyTop: "#0e1013", skyBottom: "#1d2a35", mountainBack: "#1a232c", mountainFront: "#25323d", ground: "#11171c", accent: "#8fb3c9", stars: 0, ambient: "none" },
  { skyTop: "#130a0a", skyBottom: "#3a1612", mountainBack: "#2a1311", mountainFront: "#3a1c17", ground: "#1a0d0b", accent: "#e0624d", stars: 0, ambient: "none" },
  { skyTop: "#0a1018", skyBottom: "#1c3a5c", mountainBack: "#14233a", mountainFront: "#1e3552", ground: "#0c1624", accent: "#5ab0ff", stars: 14, ambient: "none" },
  { skyTop: "#06100f", skyBottom: "#0b3b36", mountainBack: "#0b2724", mountainFront: "#0f3733", ground: "#07171a", accent: "#19e6c1", stars: 22, ambient: "glow" },
  { skyTop: "#0a0716", skyBottom: "#2a1250", mountainBack: "#1a1033", mountainFront: "#25164a", ground: "#0e0a1e", accent: "#b07cff", stars: 34, ambient: "glow" },
  { skyTop: "#000000", skyBottom: "#1a1400", mountainBack: "#0f0d05", mountainFront: "#1a1608", ground: "#080703", accent: "#ffd45a", stars: 46, ambient: "glow" }
];

export function paletteFor(rankIndex: number): RankPalette {
  return RANK_PALETTES[Math.min(Math.max(rankIndex, 0), RANK_PALETTES.length - 1)];
}

// ------------------------------------------------------------------ item art (emoji tiles for v1)

const ITEM_GLYPHS: Record<string, string> = {
  // basics
  "grass-patch": "🌱", tree: "🌳", rock: "⛰️", "dirt-path": "🛤️", fence: "🚧", barrel: "🛢️", "flower-bed": "🌷",
  "cobblestone-road": "🛣️", "lamp-post": "🏮", bench: "🛋️", signpost: "📍", "hay-bale": "🌾", "market-crate": "📦",
  "traffic-light": "🚦", sidewalk: "🚶", billboard: "📺", "bus-stop": "🚏", "street-trees": "🌲", "power-lines": "⚡",
  "solar-panel": "🔆", "sensor-post": "📡", "drone-pad": "🚁", "light-path": "✨", "holo-sign": "🔷", "hover-pad": "⭕",
  "energy-lamp": "💡", "plasma-path": "🔮", "holo-garden": "🌿", "portal-marker": "🌀",
  // structures
  tent: "⛺", campfire: "🔥", well: "⛲", "storage-shed": "🏚️", hut: "🏕️", "farm-plot": "🥕",
  "wooden-house": "🏠", bakery: "🥖", "market-stall": "🏪", "stone-house": "🏡", bridge: "🌉", school: "🏫",
  apartments: "🏢", factory: "🏭", "train-track": "🚆", skyscraper: "🏙️", mall: "🏬", hospital: "🏥",
  "smart-home": "🏘️", "charging-hub": "🔋", "tech-lab": "🔬", "sky-pod": "🚡", "floating-home": "☁️", "vertical-farm": "🌽",
  "quantum-lab": "⚛️", "floating-villa": "🏝️", "orbital-dock": "🛰️",
  // landmarks (gate + bonus)
  watchtower: "🗼", "town-hall": "🏛️", "city-gate": "⛩️", windmill: "⚙️", "clock-tower": "🕰️",
  "railway-station": "🚉", airport: "✈️", "space-tech-tower": "🚀", "sky-bridge": "🌉", "teleport-gate": "🌌",
  "great-barn": "🐄", "beacon-tower": "🔦", castle: "🏰", cathedral: "⛪", stadium: "🏟️", "observation-tower": "🔭",
  "floating-district-hub": "🌆", "drone-port": "🛸", "space-port": "🌠", "singularity-core": "🌟"
};

const TRACK_FALLBACK_GLYPH: Record<TrackName, string> = { BASICS: "🌿", STRUCTURES: "🏠", LANDMARKS: "🗼" };

/** Catalog keys look like "e2.wooden-house"; the part after the dot picks the glyph. */
export function itemGlyph(key: string, track: TrackName): string {
  const slug = key.includes(".") ? key.slice(key.indexOf(".") + 1) : key;
  return ITEM_GLYPHS[slug] ?? TRACK_FALLBACK_GLYPH[track];
}

// ------------------------------------------------------------------ deterministic city layout

export type Slot = { x: number; y: number; layer: number };

/** Scene coordinates: viewBox 0 0 800 420, ground starts near y = 262. */
export const SCENE_WIDTH = 800;
export const SCENE_HEIGHT = 420;

/** How many items one "layer" of a track holds. Later layers sit slightly offset on top of earlier ones. */
export const SLOT_CAPACITY: Record<TrackName, number> = { LANDMARKS: 10, STRUCTURES: 24, BASICS: 40 };
export const MAX_LAYERS = 3;

const CENTER_OUT_10 = [4, 5, 3, 6, 2, 7, 1, 8, 0, 9];
const CENTER_OUT_12 = [5, 6, 4, 7, 3, 8, 2, 9, 1, 10, 0, 11];

/**
 * Position of the n-th completed item of a track. A pure function of (track, sequenceNumber), so every nation
 * looks structurally the same at the same progress. Landmarks take prominent pads at the back, structures fill
 * two rows of plots from the center outwards, basics scatter along the front.
 */
export function layoutSlot(track: TrackName, sequenceNumber: number): Slot {
  const i = Math.max(0, sequenceNumber - 1);
  const perLayer = SLOT_CAPACITY[track];
  const index = i % perLayer;
  const layer = Math.floor(i / perLayer) % MAX_LAYERS;

  let x: number;
  let y: number;
  if (track === "LANDMARKS") {
    const pad = CENTER_OUT_10[index];
    x = 55 + pad * (690 / 9);
    y = 300 - (pad % 2) * 8;
  } else if (track === "STRUCTURES") {
    const col = CENTER_OUT_12[Math.floor(index / 2)];
    const row = index % 2;
    x = 45 + col * (710 / 11);
    y = 338 + row * 28;
  } else {
    const slot = (index * 17) % 40; // 17 is coprime with 40: a fixed scattered order that visits every slot
    x = 20 + slot * (760 / 39);
    y = 396 + ((slot % 3) - 1) * 4;
  }
  return { x: x + layer * 7, y: y - layer * 5, layer };
}

// ------------------------------------------------------------------ what one accepted submission did

export type NationResultDto = {
  /** FIRST: full award. RESOLVE: tiny re-solve bonus. NO_OP: nothing awarded (see reason). */
  mode: "FIRST" | "RESOLVE" | "NO_OP";
  reason: "ALREADY_COUNTED" | "AWARDS_DISABLED" | null;
  xpGained: number;
  basicsPoints: number;
  structuresPoints: number;
  landmarksPoints: number;
  itemsCompleted: { track: TrackName; key: string; name: string }[];
  /** Names of the ranks reached by this solve, in order (usually empty, rarely more than one). */
  rankUps: string[];
  rankName: string | null;
  tier: number;
  nationXp: number;
  tracks: TrackProgressDto[];
  /** Cosmetics that became available through the rank-up(s) of this solve. */
  unlocked: { category: string; key: string; name: string }[];
};
