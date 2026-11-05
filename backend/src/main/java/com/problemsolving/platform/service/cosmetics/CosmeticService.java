package com.problemsolving.platform.service.cosmetics;

import com.problemsolving.platform.api.dto.cosmetics.CosmeticItemDto;
import com.problemsolving.platform.api.dto.cosmetics.CosmeticsDto;
import com.problemsolving.platform.api.dto.cosmetics.EquipRequest;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
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

/**
 * Which cosmetics a nation owns and which ones it uses. Rank cosmetics are owned automatically once the rank is
 * reached (derived from the rank, so there is nothing to back-fill and nothing to get out of sync). Cache and
 * shard cosmetics will need stored ownership in Phase 6.
 */
@Service
public class CosmeticService {

    /** Categories the owner can choose between in this phase. */
    private static final List<CosmeticCategory> EQUIPPABLE = List.of(CosmeticCategory.THEME, CosmeticCategory.BORDER);

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
                throw bad("Locked: reach rank " + props.rankName(item.unlockRank()) + " to unlock " + item.displayName());
            }
            pinned = item.key();
        }

        if (category == CosmeticCategory.THEME) {
            nation.setEquippedThemeKey(pinned);
        } else {
            nation.setEquippedBorderKey(pinned);
        }
        nation.setUpdatedAt(now);
        store.saveNation(nation);
        return view(nation);
    }

    // ------------------------------------------------------------------ shared helpers

    /** The cosmetic in effect: the owner's pick if they made one, otherwise the one of the current rank. */
    public static String effectiveKey(Nation nation, CosmeticCategory category) {
        int rank = nation == null ? 0 : nation.getRankIndex();
        String pinned = null;
        if (nation != null) {
            pinned = category == CosmeticCategory.THEME ? nation.getEquippedThemeKey() : nation.getEquippedBorderKey();
        }
        if (pinned != null && CosmeticCatalog.find(pinned).filter(item -> isOwnedAtRank(rank, item)).isPresent()) {
            return pinned;
        }
        return CosmeticCatalog.forRank(category, rank).key();
    }

    static boolean isOwnedAtRank(int rank, CosmeticCatalog.Item item) {
        return item.source() == CosmeticSource.RANK && item.unlockRank() <= rank;
    }

    // ------------------------------------------------------------------ internals

    private boolean isOwned(Nation nation, CosmeticCatalog.Item item) {
        return isOwnedAtRank(nation == null ? 0 : nation.getRankIndex(), item);
    }

    private CosmeticsDto view(Nation nation) {
        String themeKey = effectiveKey(nation, CosmeticCategory.THEME);
        String borderKey = effectiveKey(nation, CosmeticCategory.BORDER);
        List<CosmeticItemDto> items = new ArrayList<>();
        for (CosmeticCatalog.Item item : CosmeticCatalog.all()) {
            boolean equipped = item.key().equals(themeKey) || item.key().equals(borderKey);
            items.add(toDto(item, isOwned(nation, item), equipped));
        }
        return new CosmeticsDto(
                themeKey,
                borderKey,
                nation == null ? null : nation.getEquippedThemeKey(),
                nation == null ? null : nation.getEquippedBorderKey(),
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
