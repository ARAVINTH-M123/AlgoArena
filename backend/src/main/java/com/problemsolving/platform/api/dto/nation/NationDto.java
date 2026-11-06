package com.problemsolving.platform.api.dto.nation;

import com.problemsolving.platform.api.dto.cosmetics.FlagDto;
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
        /** The editor theme, frame and anthem in effect for this nation (see /api/cosmetics). */
        String themeKey,
        String borderKey,
        String anthemKey,
        /** Scene cosmetics in effect (Phase 6); null when the nation uses the rank's default look. */
        String skyKey,
        String auraKey,
        String ambientKey,
        /** The flag in effect (never null: nations without a custom flag get the default one for their rank). */
        FlagDto flag,
        /** Streak and daily-goal progress (Phase 7); zeros for users without a nation yet. */
        Retention retention) {

    /** What is still needed to reach the next rank; null when the nation is at the top rank. */
    public record NextRank(String name, long xpRequired, int percentToNext, List<Requirement> requirements) {
    }

    /** One rank requirement. type: XP, MEDIUMS, HARDS, ACTIVE_DAYS or GATE_LANDMARK. */
    public record Requirement(String type, String label, long current, long required, boolean met) {
    }

    public record Solves(int easy, int medium, int hard, long activeDays) {
    }

    /** Phase 7 retention snapshot: the streak and how far today's daily goal has come. */
    public record Retention(int streakDays, int bestStreakDays, int solvesToday, int dailyGoalTarget) {
    }

    public record CompletedItemDto(String track, String key, String name, int era, int sequenceNumber, int instance) {
    }
}
