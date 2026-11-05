package com.problemsolving.platform.service.presence;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Where live sessions are kept (userId -> sessionId -> state). The default implementation is in memory,
 * which is correct for a single backend instance. Running several instances would need a shared
 * implementation (for example Redis with TTL keys) behind this same interface.
 */
public interface PresenceStore {

    /** One browser tab / device. lastActiveAt is null until the user has shown activity in that session. */
    record SessionState(Instant lastHeartbeatAt, Instant lastActiveAt) {
    }

    Optional<SessionState> session(Long userId, String sessionId);

    void saveSession(Long userId, String sessionId, SessionState state);

    void removeSession(Long userId, String sessionId);

    /** Snapshot copy of the user's sessions (never null). */
    Map<String, SessionState> sessions(Long userId);

    Set<Long> userIds();
}
