package com.problemsolving.platform.api.dto.nation;

import com.problemsolving.platform.api.dto.presence.PresenceDto;

import java.util.List;

/**
 * Read model of a user's nation. All point values are micro-points (1 point = 1,000,000);
 * the UI shows them floored to whole points. Dates are ISO-8601 strings (UTC).
 */
public record NationDto(
        String username,
        String name,
        boolean owner,
        int rankIndex,
        String rankName,
        int tier,
        boolean ascended,
        long xp,
        NextRank nextRank,
        Solves solves,
        List<NationResultDto.TrackProgress> tracks,
        List<CompletedItemDto> completedItems,
        String timeZone,
        String renameAvailableAt,
        String timeZoneChangeAvailableAt,
        PresenceDto presence,
        boolean appearOffline,
        /** The editor theme and frame in effect for this nation (see /api/cosmetics). */
        String themeKey,
        String borderKey) {

    /** What is still needed to reach the next rank; null when the nation is at the top rank. */
    public record NextRank(String name, long xpRequired, int percentToNext, List<Requirement> requirements) {
    }

    /** One rank requirement. type: XP, MEDIUMS, HARDS, ACTIVE_DAYS or GATE_LANDMARK. */
    public record Requirement(String type, String label, long current, long required, boolean met) {
    }

    public record Solves(int easy, int medium, int hard, long activeDays) {
    }

    public record CompletedItemDto(String track, String key, String name, int era, int sequenceNumber, int instance) {
    }
}
