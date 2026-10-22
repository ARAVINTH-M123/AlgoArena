package com.problemsolving.platform.api.dto.nation;

public record NationTrackProgressDto(
        String track,
        String currentItemName,
        Integer progressPoints,
        Integer itemCost,
        Integer percent,
        String constructionStage
) {
}
