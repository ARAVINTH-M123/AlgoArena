package com.problemsolving.platform.api.dto.presence;

/**
 * What a viewer sees of a user's presence.
 * status: ONLINE, IDLE or OFFLINE. lastActiveAt (ISO-8601, UTC) is only set when OFFLINE and visible.
 * justSolvedSecondsLeft counts down from 60 after an accepted first-solve (0 = not glowing).
 */
public record PresenceDto(String status, String lastActiveAt, int justSolvedSecondsLeft) {

    public static PresenceDto offline() {
        return new PresenceDto("OFFLINE", null, 0);
    }
}
