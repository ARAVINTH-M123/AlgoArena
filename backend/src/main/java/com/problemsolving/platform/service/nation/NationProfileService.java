package com.problemsolving.platform.service.nation;

import com.problemsolving.platform.api.dto.nation.NationDto;
import com.problemsolving.platform.api.dto.nation.NationResultDto;
import com.problemsolving.platform.api.dto.nation.UpdateNationProfileRequest;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.entity.NationCompletedItem;
import com.problemsolving.platform.domain.entity.NationTrackState;
import com.problemsolving.platform.domain.enums.CosmeticCategory;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.service.cosmetics.CosmeticService;
import com.problemsolving.platform.service.presence.PresenceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Read model and profile settings (name, time zone) of a nation. Never awards anything. */
@Service
public class NationProfileService {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9 '\\-]+$");
    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 24;
    private static final NationTrack[] TRACKS = NationTrack.values();

    private final NationStore store;
    private final NationProperties props;
    private final PresenceService presence;

    public NationProfileService(NationStore store, NationProperties props, PresenceService presence) {
        this.store = store;
        this.props = props;
        this.presence = presence;
    }

    /**
     * Nation of the given user. A user who has not solved anything yet has no row; they get an empty
     * default nation (nothing is created by reading).
     */
    @Transactional(readOnly = true)
    public NationDto view(String username, boolean owner, Instant now) {
        Long userId = store.findUserId(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Nation nation = store.findNation(userId).orElse(null);
        return build(userId, username, nation, owner, now);
    }

    @Transactional
    public NationDto updateProfile(String username, UpdateNationProfileRequest request, Instant now) {
        Long userId = store.findUserId(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Nation nation = store.lockOrCreateNation(userId);

        // validate everything first so a bad field changes nothing
        String newName = null;
        if (request.getName() != null) {
            String candidate = validateName(request.getName());
            if (!candidate.equals(nation.getName())) {
                requireCooldownPassed(nation.getLastRenameAt(), props.getRenameCooldownDays(), now, "rename your nation");
                newName = candidate;
            }
        }
        String newZone = null;
        if (request.getTimeZone() != null) {
            String zone = request.getTimeZone().trim();
            if (!ZoneId.getAvailableZoneIds().contains(zone)) {
                throw bad("Unknown time zone");
            }
            if (!zone.equals(nation.getTimeZone())) {
                requireCooldownPassed(nation.getLastTimeZoneChangeAt(), props.getTimeZoneChangeCooldownDays(), now,
                        "change your time zone");
                newZone = zone;
            }
        }

        if (newName != null) {
            nation.setName(newName);
            nation.setLastRenameAt(now);
        }
        if (newZone != null) {
            nation.setTimeZone(newZone);
            nation.setLastTimeZoneChangeAt(now);
        }
        Boolean requestedAppear = request.getAppearOffline();
        boolean appearChanged = requestedAppear != null && requestedAppear != nation.isAppearOffline();
        if (appearChanged) {
            nation.setAppearOffline(requestedAppear);
        }

        if (newName != null || newZone != null || appearChanged) {
            nation.setUpdatedAt(now);
            store.saveNation(nation);
        }
        if (appearChanged) {
            presence.setAppearOffline(userId, requestedAppear, now);   // visitors see the change right away
        }
        return build(userId, username, nation, true, now);
    }

    // ---------------------------------------------------------------- validation

    private String validateName(String raw) {
        String name = raw.replace('\u2019', '\'').trim().replaceAll("\\s+", " ");
        if (name.length() < NAME_MIN || name.length() > NAME_MAX) {
            throw bad("Nation name must be " + NAME_MIN + " to " + NAME_MAX + " characters");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw bad("Nation name can only contain letters, digits, spaces, hyphens and apostrophes");
        }
        if (props.containsBlockedWord(name)) {
            throw bad("That nation name is not allowed");
        }
        return name;
    }

    private void requireCooldownPassed(Instant lastChange, int cooldownDays, Instant now, String action) {
        if (lastChange == null) {
            return;
        }
        Instant availableAt = lastChange.plus(Duration.ofDays(cooldownDays));
        if (now.isBefore(availableAt)) {
            throw bad("You can " + action + " again after " + availableAt.atZone(ZoneOffset.UTC).toLocalDate() + " (UTC)");
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    // ---------------------------------------------------------------- read model

    private NationDto build(Long userId, String username, Nation nation, boolean owner, Instant now) {
        int rank = nation == null ? 0 : nation.getRankIndex();
        long xp = nation == null ? 0L : nation.getXp();
        int era = NationRules.contentEra(rank);

        Map<NationTrack, NationTrackState> byTrack = new EnumMap<>(NationTrack.class);
        List<NationCompletedItem> completedRows = List.of();
        long activeDays = 0L;
        if (nation != null) {
            for (NationTrackState state : store.findTrackStates(nation.getId())) {
                byTrack.put(state.getTrack(), state);
            }
            completedRows = store.findCompletedItems(nation.getId());
            activeDays = store.countActiveDays(nation.getId());
        }

        List<NationResultDto.TrackProgress> tracks = new ArrayList<>();
        for (NationTrack track : TRACKS) {
            NationTrackState state = byTrack.get(track);
            String key = state != null ? state.getCurrentItemKey() : NationCatalog.initialItemKey(track);
            long progress = state != null ? state.getProgress() : 0L;
            long cost = props.itemCostMicro(era, track);
            tracks.add(new NationResultDto.TrackProgress(
                    track.name(), key, NationCatalog.find(key).name(), progress, cost,
                    NationRules.stage(progress, cost)));
        }

        boolean gateDone = byTrack.containsKey(NationTrack.LANDMARKS) && byTrack.get(NationTrack.LANDMARKS).isGateDone();
        int mediums = nation == null ? 0 : nation.getMediumSolves();
        int hards = nation == null ? 0 : nation.getHardSolves();

        List<NationDto.CompletedItemDto> completed = completedRows.stream()
                .sorted(Comparator.comparing(NationCompletedItem::getCompletedAt)
                        .thenComparing(item -> item.getTrack().ordinal())
                        .thenComparingInt(NationCompletedItem::getSequenceNumber))
                .map(item -> new NationDto.CompletedItemDto(
                        item.getTrack().name(),
                        item.getCatalogKey(),
                        NationCatalog.find(item.getCatalogKey()).name(),
                        item.getContentEra(),
                        item.getSequenceNumber(),
                        item.getInstanceNumber()))
                .toList();

        String renameAvailableAt = null;
        String zoneAvailableAt = null;
        if (owner && nation != null) {
            renameAvailableAt = availableAt(nation.getLastRenameAt(), props.getRenameCooldownDays(), now);
            zoneAvailableAt = availableAt(nation.getLastTimeZoneChangeAt(), props.getTimeZoneChangeCooldownDays(), now);
        }

        return new NationDto(
                username,
                nation == null ? username + "'s Land" : nation.getName(),
                owner,
                rank,
                props.rankName(rank),
                NationRules.tierIndex(xp, rank, props) + 1,
                nation != null && nation.isAscended(),
                xp,
                nextRank(rank, xp, mediums, hards, activeDays, gateDone),
                new NationDto.Solves(
                        nation == null ? 0 : nation.getEasySolves(), mediums, hards, activeDays),
                tracks,
                completed,
                nation == null ? "UTC" : nation.getTimeZone(),
                renameAvailableAt,
                zoneAvailableAt,
                presence.forViewer(userId, nation, owner, now),
                owner && nation != null && nation.isAppearOffline(),
                CosmeticService.effectiveKey(nation, CosmeticCategory.THEME),
                CosmeticService.effectiveKey(nation, CosmeticCategory.BORDER));
    }

    private NationDto.NextRank nextRank(int rank, long xp, int mediums, int hards, long activeDays, boolean gateDone) {
        if (rank >= NationProperties.MAX_RANK_INDEX) {
            return null;
        }
        int next = rank + 1;
        long low = props.rankXpMicro(rank);
        long high = props.rankXpMicro(next);
        int percent = (int) Math.min(100L, Math.max(0L, ((xp - low) * 100L) / (high - low)));

        List<NationDto.Requirement> requirements = new ArrayList<>();
        requirements.add(new NationDto.Requirement("XP", "Nation XP", xp, high, xp >= high));
        if (props.rankMinMediums(next) > 0) {
            requirements.add(new NationDto.Requirement("MEDIUMS", "Medium problems solved",
                    mediums, props.rankMinMediums(next), mediums >= props.rankMinMediums(next)));
        }
        if (props.rankMinHards(next) > 0) {
            requirements.add(new NationDto.Requirement("HARDS", "Hard problems solved",
                    hards, props.rankMinHards(next), hards >= props.rankMinHards(next)));
        }
        if (props.rankMinActiveDays(next) > 0) {
            requirements.add(new NationDto.Requirement("ACTIVE_DAYS", "Active days",
                    activeDays, props.rankMinActiveDays(next), activeDays >= props.rankMinActiveDays(next)));
        }
        requirements.add(new NationDto.Requirement("GATE_LANDMARK",
                "Build the " + NationCatalog.gate(rank).name(), gateDone ? 1 : 0, 1, gateDone));

        return new NationDto.NextRank(props.rankName(next), high, percent, requirements);
    }

    private static String availableAt(Instant lastChange, int cooldownDays, Instant now) {
        if (lastChange == null) {
            return null;
        }
        Instant availableAt = lastChange.plus(Duration.ofDays(cooldownDays));
        return now.isBefore(availableAt) ? availableAt.toString() : null;
    }
}
