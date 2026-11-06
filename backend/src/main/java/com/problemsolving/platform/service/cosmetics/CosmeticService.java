package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.api.dto.cosmetics.CosmeticItemDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticsDto;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
import com.problemsolving.platform.api.dto.cosmetics.FlagDto;
import com.problemsolving.platform.api.dto.cosmetics.FlagRequest;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationCosmeticOwned;
import com.problemsolving.platform.domain.enums.CosmeticCategory;
import com.problemsolving.platform.domain.enums.CosmeticSource;
import com.problemsolving.platform.service.nation.NationStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which cosmetics a nation owns and which ones it uses. Rank cosmetics are owned automatically once the rank is
 * reached (derived from the rank, so there is nothing to back-fill and nothing to get out of sync). Cache, shard
 * and special cosmetics (Phase 6) are owned through nation_cosmetic_owned rows written by the CacheService.
 */
@Service
public class CosmeticService {

    /** Categories the owner can choose between. */
    private static final List<CosmeticCategory> EQUIPPABLE = List.of(
            CosmeticCategory.THEME, CosmeticCategory.BORDER, CosmeticCategory.ANTHEM,
            CosmeticCategory.SKY, CosmeticCategory.AURA, CosmeticCategory.AMBIENT);

    /** Categories WITHOUT a per-rank default: nothing equipped means "none" (the rank's plain look). */
    private static final List<CosmeticCategory> NO_DEFAULT = List.of(
            CosmeticCategory.SKY, CosmeticCategory.AURA, CosmeticCategory.AMBIENT);

    /** Rank names are fixed defaults (yaml cannot override them), so a static copy is safe for flag messages. */
    private static final NationProperties RANK_NAMES = new NationProperties();

    private final NationStore store;
    private final NationProperties props;

    public CosmeticService(NationStore store, NationProperties props) {
        this.store = store;
        this.props = props;
    }

    /** The whole catalog, as seen by someone who owns nothing but rank 0 (public, for browsing). */
    public List<CosmeticItemDto> catalog() {
        List<CosmeticItemDto> items = new ArrayList<>();
        for (CosmeticCatalog.Item item : CosmeticCatalog.all()) {
            items.add(toDto(item, false, false));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public CosmeticsDto mine(String username) {
        Long userId = resolveUserId(username);
        return view(store.findNation(userId).orElse(null));
    }

    @Transactional
    public CosmeticsDto equip(String username, EquipRequest request, Instant now) {
        Long userId = resolveUserId(username);
        CosmeticCategory category = parseCategory(request.getCategory());
        Nation nation = store.lockOrCreateNation(userId);

        String key = request.getKey() == null ? "" : request.getKey().trim();
        String pinned;
        if (key.isEmpty() || key.equalsIgnoreCase("auto")) {
            pinned = null;
        } else {
            CosmeticCatalog.Item item = CosmeticCatalog.find(key)
                    .filter(found -> found.category() == category)
                    .orElseThrow(() -> bad("Unknown cosmetic"));
            if (!isOwned(nation, item)) {
                if (item.source() == CosmeticSource.RANK) {
                    throw bad("Locked: reach rank " + props.rankName(item.unlockRank()) + " to unlock " + item.displayName());
                }
                throw bad("Locked: " + item.displayName() + " is not in your collection yet (open caches or craft it)");
            }
            pinned = item.key();
        }

        switch (category) {
            case THEME -> nation.setEquippedThemeKey(pinned);
            case BORDER -> nation.setEquippedBorderKey(pinned);
            case ANTHEM -> nation.setEquippedAnthemKey(pinned);
            case SKY -> nation.setEquippedSkyKey(pinned);
            case AURA -> nation.setEquippedAuraKey(pinned);
            case AMBIENT -> nation.setEquippedAmbientKey(pinned);
            default -> throw bad("That category cannot be equipped");
        }
        nation.setUpdatedAt(now);
        store.saveNation(nation);
        return view(nation);
    }

    // ------------------------------------------------------------------ shared helpers

    /**
     * The cosmetic in effect: the owner's pick if they made one, otherwise the one of the current rank.
     * Categories without a rank default (SKY/AURA/AMBIENT) return null when nothing valid is pinned.
     */
    public static String effectiveKey(Nation nation, CosmeticCategory category) {
        return effectiveKey(nation, category, Set.of());
    }

    /** Same, but also honors acquired-cosmetic ownership rows (Phase 6). */
    public static String effectiveKey(Nation nation, CosmeticCategory category, Set<String> ownedKeys) {
        int rank = nation == null ? 0 : nation.getRankIndex();
        String pinned = null;
        if (nation != null) {
            pinned = switch (category) {
                case THEME -> nation.getEquippedThemeKey();
                case BORDER -> nation.getEquippedBorderKey();
                case ANTHEM -> nation.getEquippedAnthemKey();
                case SKY -> nation.getEquippedSkyKey();
                case AURA -> nation.getEquippedAuraKey();
                case AMBIENT -> nation.getEquippedAmbientKey();
                default -> null;
            };
        }
        if (pinned != null) {
            final String pinnedKey = pinned;
            boolean owned = CosmeticCatalog.find(pinnedKey)
                    .map(item -> item.source() == CosmeticSource.RANK
                            ? isOwnedAtRank(rank, item)
                            : ownedKeys.contains(pinnedKey))
                    .orElse(false);
            if (owned) {
                return pinned;
            }
        }
        if (NO_DEFAULT.contains(category)) {
            return null;
        }
        return CosmeticCatalog.forRank(category, rank).key();
    }

    static boolean isOwnedAtRank(int rank, CosmeticCatalog.Item item) {
        return item.source() == CosmeticSource.RANK && item.unlockRank() <= rank;
    }

    // ------------------------------------------------------------------ flag (Phase 5B)

    /** The flag every nation shows until the owner picks their own: horizontal slate over charcoal with the emblem of the current rank. */
    public static FlagDto defaultFlag(int rankIndex) {
        return new FlagDto(
                "pattern.horizontal",
                List.of("color.slate", "color.charcoal"),
                CosmeticCatalog.forRank(CosmeticCategory.FLAG_EMBLEM, rankIndex).key());
    }

    /**
     * The flag in effect for a nation: the stored one if the owner ever set it, otherwise the default of the
     * current rank. Stored flags were validated when saved; anything malformed falls back to the default.
     */
    public static FlagDto flagOf(Nation nation) {
        int rank = nation == null ? 0 : nation.getRankIndex();
        String pattern = nation == null ? null : nation.getFlagPattern();
        String colors = nation == null ? null : nation.getFlagColors();
        if (pattern == null || pattern.isBlank() || colors == null || colors.isBlank()) {
            return defaultFlag(rank);
        }
        List<String> colorList = List.of(colors.split(","));
        String emblem = nation.getFlagEmblem();
        boolean shapeKnown = CosmeticCatalog.find(pattern)
                .filter(item -> item.category() == CosmeticCategory.FLAG_PATTERN).isPresent();
        boolean colorsKnown = !colorList.isEmpty() && colorList.stream().allMatch(color -> CosmeticCatalog.find(color)
                .filter(item -> item.category() == CosmeticCategory.FLAG_COLOR).isPresent());
        boolean emblemKnown = emblem == null || CosmeticCatalog.find(emblem)
                .filter(item -> item.category() == CosmeticCategory.FLAG_EMBLEM).isPresent();
        if (!shapeKnown || !colorsKnown || !emblemKnown) {
            return defaultFlag(rank);
        }
        return new FlagDto(pattern, colorList, emblem);
    }

    /**
     * Validates a flag request against the catalog and the nation's rank and returns the normalized flag.
     * Static so the profile service can validate the flag BEFORE applying name, zone or anything else
     * (a bad field must never change anything).
     */
    public static FlagDto validateFlag(int rankIndex, FlagRequest request) {
        if (request == null) {
            throw bad("Unknown flag pattern");
        }

        String patternKey = trimmed(request.getPattern());
        CosmeticCatalog.Item pattern = CosmeticCatalog.find(patternKey)
                .filter(item -> item.category() == CosmeticCategory.FLAG_PATTERN)
                .orElseThrow(() -> bad("Unknown flag pattern"));
        requireUnlocked(rankIndex, pattern);

        List<String> rawColors = request.getColors() == null ? List.of() : request.getColors();
        List<String> colors = new ArrayList<>();
        for (String rawColor : rawColors) {
            String colorKey = trimmed(rawColor);
            CosmeticCatalog.Item color = CosmeticCatalog.find(colorKey)
                    .filter(item -> item.category() == CosmeticCategory.FLAG_COLOR)
                    .orElseThrow(() -> bad("Unknown flag color"));
            requireUnlocked(rankIndex, color);
            if (!colors.contains(color.key())) {
                colors.add(color.key());          // the same color twice is one color
            }
        }
        if (colors.size() < 2 || colors.size() > 3) {
            throw bad("A flag needs 2 or 3 colors");   // counted AFTER deduping: an unknown color is reported first
        }

        String emblemKey = trimmed(request.getEmblem());
        if (!emblemKey.isEmpty()) {
            CosmeticCatalog.Item emblem = CosmeticCatalog.find(emblemKey)
                    .filter(item -> item.category() == CosmeticCategory.FLAG_EMBLEM)
                    .orElseThrow(() -> bad("Unknown flag emblem"));
            requireUnlocked(rankIndex, emblem);
            return new FlagDto(pattern.key(), List.copyOf(colors), emblem.key());
        }
        return new FlagDto(pattern.key(), List.copyOf(colors), null);
    }

    private static void requireUnlocked(int rankIndex, CosmeticCatalog.Item item) {
        if (!isOwnedAtRank(rankIndex, item)) {
            throw bad("Locked: reach rank " + RANK_NAMES.rankName(item.unlockRank()) + " to unlock " + item.displayName());
        }
    }

    private static String trimmed(String raw) {
        return raw == null ? "" : raw.trim();
    }

    // ------------------------------------------------------------------ internals

    /** Rank cosmetics are derived from the rank; everything else needs an ownership row. */
    private boolean isOwned(Nation nation, CosmeticCatalog.Item item) {
        if (item.source() == CosmeticSource.RANK) {
            return isOwnedAtRank(nation == null ? 0 : nation.getRankIndex(), item);
        }
        return nation != null && store.ownsCosmetic(nation.getUserId(), item.key());
    }

    private CosmeticsDto view(Nation nation) {
        Set<String> ownedKeys = nation == null
                ? Set.of()
                : Set.copyOf(store.findOwnedCosmetics(nation.getUserId()).stream()
                        .map(NationCosmeticOwned::getCosmeticKey).toList());
        String themeKey = effectiveKey(nation, CosmeticCategory.THEME, ownedKeys);
        String borderKey = effectiveKey(nation, CosmeticCategory.BORDER, ownedKeys);
        String anthemKey = effectiveKey(nation, CosmeticCategory.ANTHEM, ownedKeys);
        String skyKey = effectiveKey(nation, CosmeticCategory.SKY, ownedKeys);
        String auraKey = effectiveKey(nation, CosmeticCategory.AURA, ownedKeys);
        String ambientKey = effectiveKey(nation, CosmeticCategory.AMBIENT, ownedKeys);
        FlagDto flag = flagOf(nation);
        List<CosmeticItemDto> items = new ArrayList<>();
        for (CosmeticCatalog.Item item : CosmeticCatalog.all()) {
            boolean owned = item.source() == CosmeticSource.RANK
                    ? isOwnedAtRank(nation == null ? 0 : nation.getRankIndex(), item)
                    : ownedKeys.contains(item.key());
            boolean equipped = item.key().equals(themeKey) || item.key().equals(borderKey)
                    || item.key().equals(anthemKey)
                    || item.key().equals(skyKey) || item.key().equals(auraKey) || item.key().equals(ambientKey)
                    || (item.category() == CosmeticCategory.FLAG_PATTERN && item.key().equals(flag.pattern()))
                    || (item.category() == CosmeticCategory.FLAG_COLOR && flag.colors().contains(item.key()))
                    || (item.category() == CosmeticCategory.FLAG_EMBLEM && item.key().equals(flag.emblem()));
            items.add(toDto(item, owned, equipped));
        }
        return new CosmeticsDto(
                themeKey,
                borderKey,
                nation == null ? null : nation.getEquippedThemeKey(),
                nation == null ? null : nation.getEquippedBorderKey(),
                anthemKey,
                nation == null ? null : nation.getEquippedAnthemKey(),
                skyKey,
                nation == null ? null : nation.getEquippedSkyKey(),
                auraKey,
                nation == null ? null : nation.getEquippedAuraKey(),
                ambientKey,
                nation == null ? null : nation.getEquippedAmbientKey(),
                flag,
                items);
    }

    private CosmeticItemDto toDto(CosmeticCatalog.Item item, boolean owned, boolean equipped) {
        Integer unlockRank = item.unlockRank();
        return new CosmeticItemDto(
                item.key(),
                item.category().name(),
                item.rarity().name(),
                item.displayName(),
                unlockRank,
                unlockRank == null ? null : props.rankName(unlockRank),
                owned,
                equipped);
    }

    private CosmeticCategory parseCategory(String raw) {
        CosmeticCategory category;
        try {
            category = CosmeticCategory.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw bad("Unknown cosmetic category");
        }
        if (!EQUIPPABLE.contains(category)) {
            throw bad("That category cannot be equipped yet");
        }
        return category;
    }

    private Long resolveUserId(String username) {
        return store.findUserId(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
