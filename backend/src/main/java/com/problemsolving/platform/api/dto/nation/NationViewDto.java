package com.problemsolving.platform.api.dto.nation;

import java.util.List;

public record NationViewDto(
        String username,
        String rank,
        String subTier,
        String rankWithTier,
        Long xp,
        Long xpToNextRank,
        NationMissingRequirementsDto missingForNextRank,
        NationTrackProgressDto basics,
        NationTrackProgressDto structures,
        NationTrackProgressDto landmarks,
        List<NationCompletedItemDto> completedItems,
        NationPresenceDto presence
) {
}
