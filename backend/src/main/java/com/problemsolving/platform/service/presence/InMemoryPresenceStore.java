package com.problemsolving.platform.service.presence;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Default single-instance store. */
@Component
public class InMemoryPresenceStore implements PresenceStore {

    private final Map<Long, Map<String, SessionState>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public Optional<SessionState> session(Long userId, String sessionId) {
        Map<String, SessionState> sessions = sessionsByUser.get(userId);
        return sessions == null ? Optional.empty() : Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void saveSession(Long userId, String sessionId, SessionState state) {
        sessionsByUser.computeIfAbsent(userId, id -> new ConcurrentHashMap<>()).put(sessionId, state);
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        Map<String, SessionState> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId, sessions);
        }
    }

    @Override
    public Map<String, SessionState> sessions(Long userId) {
        Map<String, SessionState> sessions = sessionsByUser.get(userId);
        return sessions == null ? Map.of() : new HashMap<>(sessions);
    }

    @Override
    public Set<Long> userIds() {
        return new HashSet<>(sessionsByUser.keySet());
    }
}
