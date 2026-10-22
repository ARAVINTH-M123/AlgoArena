package com.problemsolving.platform.api.dto.nation;

import java.util.List;

public record NationAwardSummaryDto(
        boolean firstSolveCounted,
        int xpGained,
        int basicsPointsGained,
        int structuresPointsGained,
        int landmarksPointsGained,
        String rankBefore,
        String rankAfter,
        List<String> completedItems
) {
}
