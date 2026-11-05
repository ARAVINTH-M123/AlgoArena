package com.problemsolving.platform.api.dto.nation;

import java.util.List;

/**
 * What one accepted submission did to the user's nation. All point values are micro-points
 * (1 point = 1,000,000); the UI shows them floored to whole points.
 */
public record NationResultDto(
        String mode,
        long xpGained,
        long basicsPoints,
        long structuresPoints,
        long landmarksPoints,
        List<CompletedItem> itemsCompleted,
        List<String> rankUps,
        String rankName,
        int tier,
        long nationXp,
        List<TrackProgress> tracks,
        String reason,
        List<UnlockedCosmetic> unlocked) {

    /** A cosmetic that became available through a rank-up of this solve. */
    public record UnlockedCosmetic(String category, String key, String name) {
    }

    public record CompletedItem(String track, String key, String name) {
    }

    public record TrackProgress(String track, String itemKey, String itemName, long progress, long cost, int stage) {
    }

    /** Nothing was awarded because awards are switched off in this deployment. */
    public static final String REASON_AWARDS_DISABLED = "AWARDS_DISABLED";
    /** Nothing was awarded because this problem was already counted (re-solves count again after 24 hours). */
    public static final String REASON_ALREADY_COUNTED = "ALREADY_COUNTED";

    public static NationResultDto noOp(String reason) {
        return new NationResultDto("NO_OP", 0L, 0L, 0L, 0L, List.of(), List.of(), null, 0, 0L, List.of(), reason, List.of());
    }
}
