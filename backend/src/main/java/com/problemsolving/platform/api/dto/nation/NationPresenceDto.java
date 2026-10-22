package com.problemsolving.platform.api.dto.nation;

import java.time.LocalDateTime;

public record NationPresenceDto(
        String status,
        String text,
        LocalDateTime lastSeenAt,
        boolean appearOffline
) {
}
