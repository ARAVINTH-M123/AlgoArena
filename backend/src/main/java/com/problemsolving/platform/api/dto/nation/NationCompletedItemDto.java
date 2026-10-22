package com.problemsolving.platform.api.dto.nation;

import java.time.LocalDateTime;

public record NationCompletedItemDto(
        String track,
        Integer era,
        String name,
        Integer cost,
        LocalDateTime completedAt
) {
}
