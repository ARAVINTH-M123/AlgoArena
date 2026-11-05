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
 * progress. The look of each key (colors, CSS) lives in the frontend, keyed by the same strings.
 * Phase 5A: editor themes and borders (one guaranteed per rank). Flags and anthems follow in 5B.
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

    private static final List<Item> ITEMS = new ArrayList<>();
    private static final Map<String, Item> BY_KEY = new LinkedHashMap<>();

    static {
        for (int rank = 0; rank < THEMES.length; rank++) {
            add(new Item(THEMES[rank][0], CosmeticCategory.THEME, rarityOfRank(rank), CosmeticSource.RANK, rank, THEMES[rank][1]));
        }
        for (int rank = 0; rank < BORDERS.length; rank++) {
            add(new Item(BORDERS[rank][0], CosmeticCategory.BORDER, rarityOfRank(rank), CosmeticSource.RANK, rank, BORDERS[rank][1]));
        }
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
}
