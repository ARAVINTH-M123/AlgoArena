package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.domain.enums.CosmeticCategory;
import com.problemsolving.platform.domain.enums.CosmeticRarity;
import com.problemsolving.platform.domain.enums.CosmeticSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every cosmetic that exists (blueprint section 9). Cosmetics are visual only: nothing here may grant XP or
 * progress. The look of each key (colors, CSS, SVG) lives in the frontend, keyed by the same strings.
 * Phase 5A: editor themes and borders (one guaranteed per rank). Phase 5B adds anthems and flag emblems
 * (one guaranteed per rank) plus the flag parts every nation owns from the start (7 patterns, 12 colors).
 * Phase 6 adds the Mystery Cache pool: 4 skies, 4 auras, 4 ambient effects (one per rarity), plus the
 * Ascended Aura granted when the Teleport Gate completes.
 */
public final class CosmeticCatalog {

    public record Item(String key, CosmeticCategory category, CosmeticRarity rarity, CosmeticSource source,
                       Integer unlockRank, String displayName) {
    }

    /** index = rank index 0..9 */
    private static final String[][] THEMES = {
            {"theme.void", "Void"}, {"theme.ember", "Ember"}, {"theme.terminal", "Terminal"},
            {"theme.timber", "Timber"}, {"theme.stonework", "Stonework"}, {"theme.brickline", "Brickline"},
            {"theme.skyline", "Skyline"}, {"theme.circuit", "Circuit"}, {"theme.aurora", "Aurora"},
            {"theme.event-horizon", "Event Horizon"}};

    private static final String[][] BORDERS = {
            {"border.thin-gray", "Thin Gray"}, {"border.ember-glow", "Ember Glow"}, {"border.green-pixel", "Green Pixel"},
            {"border.wood-grain", "Wood Grain"}, {"border.stone", "Stone"}, {"border.riveted-metal", "Riveted Metal"},
            {"border.white-mist", "White Mist"}, {"border.circuit-traces", "Circuit Traces"},
            {"border.shimmer", "Shimmering Gradient"}, {"border.prismatic-gold", "Prismatic Gold"}};

    private static final String[][] ANTHEMS = {
            {"anthem.soft-pulse", "Soft Pulse"}, {"anthem.campfire", "Campfire"}, {"anthem.chiptune", "Chiptune"},
            {"anthem.folk", "Folk Hymn"}, {"anthem.march", "Slow March"}, {"anthem.industrial", "Industrial Heart"},
            {"anthem.airy", "Airy Skies"}, {"anthem.synth", "Synth Dawn"}, {"anthem.shimmer", "Shimmer"},
            {"anthem.ascension", "Ascension"}};

    private static final String[][] EMBLEMS = {
            {"emblem.cursor", "Cursor"}, {"emblem.seedling", "Seedling"}, {"emblem.gear", "Gear"},
            {"emblem.roots", "Roots"}, {"emblem.shield", "Shield"}, {"emblem.stack", "Stack"},
            {"emblem.cloud", "Cloud"}, {"emblem.core", "Core"}, {"emblem.atom", "Atom"}, {"emblem.spiral", "Spiral"}};

    private static final String[][] PATTERNS = {
            {"pattern.horizontal", "Horizontal"}, {"pattern.vertical", "Vertical"}, {"pattern.diagonal", "Diagonal"},
            {"pattern.cross", "Nordic Cross"}, {"pattern.saltire", "Saltire"}, {"pattern.triangle", "Hoist Triangle"},
            {"pattern.canton", "Canton"}};

    private static final String[][] COLORS = {
            {"color.slate", "Slate"}, {"color.charcoal", "Charcoal"}, {"color.white", "White"}, {"color.crimson", "Crimson"},
            {"color.orange", "Orange"}, {"color.amber", "Amber"}, {"color.green", "Green"}, {"color.teal", "Teal"},
            {"color.sky", "Sky"}, {"color.blue", "Blue"}, {"color.violet", "Violet"}, {"color.pink", "Pink"}};

    /** Mystery Cache pool (Phase 6): [key, name], one per category and rarity. Order: COMMON, RARE, EPIC, LEGENDARY. */
    private static final String[][] SKIES = {
            {"sky.meadow-dawn", "Meadow Dawn"}, {"sky.dusk-fire", "Dusk Fire"},
            {"sky.nebula", "Nebula"}, {"sky.void-veil", "Void Veil"}};
    private static final String[][] AURAS = {
            {"aura.ember-ring", "Ember Ring"}, {"aura.tidal-glow", "Tidal Glow"},
            {"aura.aurora-crown", "Aurora Crown"}, {"aura.star-shard", "Star Shard"}};
    private static final String[][] AMBIENTS = {
            {"ambient.pollen", "Pollen Drift"}, {"ambient.ember-motes", "Ember Motes"},
            {"ambient.fireflies", "Fireflies"}, {"ambient.stardust", "Stardust"}};

    private static final CosmeticRarity[] POOL_RARITIES =
            {CosmeticRarity.COMMON, CosmeticRarity.RARE, CosmeticRarity.EPIC, CosmeticRarity.LEGENDARY};

    private static final List<Item> ITEMS = new ArrayList<>();
    private static final Map<String, Item> BY_KEY = new LinkedHashMap<>();

    static {
        for (int rank = 0; rank < THEMES.length; rank++) {
            add(new Item(THEMES[rank][0], CosmeticCategory.THEME, rarityOfRank(rank), CosmeticSource.RANK, rank, THEMES[rank][1]));
        }
        for (int rank = 0; rank < BORDERS.length; rank++) {
            add(new Item(BORDERS[rank][0], CosmeticCategory.BORDER, rarityOfRank(rank), CosmeticSource.RANK, rank, BORDERS[rank][1]));
        }
        for (int rank = 0; rank < ANTHEMS.length; rank++) {
            add(new Item(ANTHEMS[rank][0], CosmeticCategory.ANTHEM, rarityOfRank(rank), CosmeticSource.RANK, rank, ANTHEMS[rank][1]));
        }
        for (int rank = 0; rank < EMBLEMS.length; rank++) {
            add(new Item(EMBLEMS[rank][0], CosmeticCategory.FLAG_EMBLEM, rarityOfRank(rank), CosmeticSource.RANK, rank, EMBLEMS[rank][1]));
        }
        // Flag patterns and colors: every nation owns them from the start (rank 0), so a rank-up always
        // unlocks exactly four cosmetics: one theme, one border, one anthem, one emblem.
        for (String[] pattern : PATTERNS) {
            add(new Item(pattern[0], CosmeticCategory.FLAG_PATTERN, CosmeticRarity.COMMON, CosmeticSource.RANK, 0, pattern[1]));
        }
        for (String[] color : COLORS) {
            add(new Item(color[0], CosmeticCategory.FLAG_COLOR, CosmeticRarity.COMMON, CosmeticSource.RANK, 0, color[1]));
        }
        // Mystery Cache pool (Phase 6): only obtainable from caches or shard crafting, never by rank.
        for (int rarity = 0; rarity < POOL_RARITIES.length; rarity++) {
            add(new Item(SKIES[rarity][0], CosmeticCategory.SKY, POOL_RARITIES[rarity], CosmeticSource.CACHE, null, SKIES[rarity][1]));
            add(new Item(AURAS[rarity][0], CosmeticCategory.AURA, POOL_RARITIES[rarity], CosmeticSource.CACHE, null, AURAS[rarity][1]));
            add(new Item(AMBIENTS[rarity][0], CosmeticCategory.AMBIENT, POOL_RARITIES[rarity], CosmeticSource.CACHE, null, AMBIENTS[rarity][1]));
        }
        // The Ascended Aura: granted once, when the Teleport Gate completes at Singularity (Phase 6).
        add(new Item("aura.ascended", CosmeticCategory.AURA, CosmeticRarity.LEGENDARY, CosmeticSource.SPECIAL, null, "Ascended Aura"));
    }

    private CosmeticCatalog() {
    }

    private static void add(Item item) {
        ITEMS.add(item);
        BY_KEY.put(item.key(), item);
    }

    private static CosmeticRarity rarityOfRank(int rank) {
        if (rank <= 4) return CosmeticRarity.COMMON;
        if (rank <= 6) return CosmeticRarity.RARE;
        if (rank <= 8) return CosmeticRarity.EPIC;
        return CosmeticRarity.LEGENDARY;
    }

    public static List<Item> all() {
        return List.copyOf(ITEMS);
    }

    public static Optional<Item> find(String key) {
        return Optional.ofNullable(key == null ? null : BY_KEY.get(key));
    }

    /** The guaranteed cosmetic of this category for a rank (rank cosmetics: exactly one per rank). */
    public static Item forRank(CosmeticCategory category, int rankIndex) {
        for (Item item : ITEMS) {
            if (item.category() == category && item.source() == CosmeticSource.RANK && item.unlockRank() == rankIndex) {
                return item;
            }
        }
        throw new IllegalStateException("No " + category + " cosmetic for rank " + rankIndex);
    }

    /** Rank cosmetics that become available when the given ranks are reached. */
    public static List<Item> unlockedAtRanks(List<Integer> rankIndexes) {
        List<Item> unlocked = new ArrayList<>();
        for (Item item : ITEMS) {
            if (item.source() == CosmeticSource.RANK && rankIndexes.contains(item.unlockRank())) {
                unlocked.add(item);
            }
        }
        return unlocked;
    }

    /** Every cosmetic of one source (the Mystery Cache pool, or the special grants). */
    public static List<Item> bySource(CosmeticSource source) {
        List<Item> found = new ArrayList<>();
        for (Item item : ITEMS) {
            if (item.source() == source) {
                found.add(item);
            }
        }
        return found;
    }

    /** The cache-pool cosmetics of one rarity, in catalog order. */
    public static List<Item> cachePool(CosmeticRarity rarity) {
        List<Item> found = new ArrayList<>();
        for (Item item : ITEMS) {
            if (item.source() == CosmeticSource.CACHE && item.rarity() == rarity) {
                found.add(item);
            }
        }
        return found;
    }

    /** True for the categories a Mystery Cache or crafting can grant (the scene cosmetics). */
    public static boolean isCacheCategory(CosmeticCategory category) {
        return category == CosmeticCategory.SKY || category == CosmeticCategory.AURA
                || category == CosmeticCategory.AMBIENT;
    }
}
