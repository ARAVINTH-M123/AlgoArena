package com.problemsolving.platform.service.presence;

import com.problemsolving.platform.api.dto.presence.PresenceDto;
import com.problemsolving.platform.config.NationProperties;
import com.problemsolving.platform.domain.entity.Nation;
import com.problemsolving.platform.domain.enums.PresenceStatus;
import com.problemsolving.platform.service.nation.NationStore;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Presence ("the online god indicator"). Truth source: heartbeats with a TTL. Delivery: listeners that the
 * SSE hub registers, notified only when a viewer-visible state actually changes.
 *
 * ONLINE  = a live session (heartbeat younger than the TTL) AND user activity within the idle window.
 * IDLE    = a live session but no recent activity.
 * OFFLINE = no live session, or the owner chose "Appear offline" (visitors only).
 */
@Service
public class PresenceService {

    private static final Pattern SESSION_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    /** What listeners were last told. Used to publish changes only. */
    private record Published(PresenceStatus status, boolean solving) {
    }

    private final PresenceStore store;
    private final NationStore nationStore;
    private final NationProperties props;

    private final Map<Long, Instant> lastPersisted = new ConcurrentHashMap<>();
    private final Map<Long, Instant> justSolvedUntil = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> appearOfflineCache = new ConcurrentHashMap<>();
    private final Map<Long, Published> lastPublished = new ConcurrentHashMap<>();
    private final Map<Long, Set<Consumer<PresenceDto>>> listeners = new ConcurrentHashMap<>();

    public PresenceService(PresenceStore store, NationStore nationStore, NationProperties props) {
        this.store = store;
        this.nationStore = nationStore;
        this.props = props;
    }

    // ------------------------------------------------------------------ input: heartbeats

    public Long resolveUserId(String username) {
        return nationStore.findUserId(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** @return false when the heartbeat came faster than the allowed interval (answer HTTP 429). */
    public boolean heartbeat(Long userId, String sessionId, boolean active, Instant now) {
        validateSessionId(sessionId);
        PresenceStore.SessionState existing = store.session(userId, sessionId).orElse(null);
        if (existing != null
                && now.isBefore(existing.lastHeartbeatAt().plusSeconds(props.getPresenceHeartbeatMinIntervalSeconds()))) {
            return false;
        }
        Instant lastActive = active ? now : (existing == null ? null : existing.lastActiveAt());
        store.saveSession(userId, sessionId, new PresenceStore.SessionState(now, lastActive));
        enforceSessionLimit(userId);
        maybePersistLastSeen(userId, now);
        publish(userId, now);
        return true;
    }

    /** Explicit "this tab is closing": drop the session now instead of waiting for the TTL. */
    public void offline(Long userId, String sessionId, Instant now) {
        validateSessionId(sessionId);
        store.removeSession(userId, sessionId);
        persistLastSeen(userId, now);
        publish(userId, now);
    }

    public boolean heartbeatByUsername(String username, String sessionId, boolean active, Instant now) {
        return heartbeat(resolveUserId(username), sessionId, active, now);
    }

    public void offlineByUsername(String username, String sessionId, Instant now) {
        offline(resolveUserId(username), sessionId, now);
    }

    /** Called after an accepted first-solve: the figure glows for a while. */
    public void markSolved(Long userId, Instant now) {
        justSolvedUntil.put(userId, now.plusSeconds(props.getPresenceJustSolvedSeconds()));
        publish(userId, now);
    }

    /** Called when the owner flips "Appear offline". Visitors are updated immediately. */
    public void setAppearOffline(Long userId, boolean appearOffline, Instant now) {
        appearOfflineCache.put(userId, appearOffline);
        publish(userId, now);
    }

    // ------------------------------------------------------------------ output: what a viewer sees

    /**
     * Presence as shown to a viewer. The owner always sees their true status; visitors see OFFLINE (and no
     * last-seen time) while "Appear offline" is on. nation may be null for users without a nation row.
     */
    public PresenceDto forViewer(Long userId, Nation nation, boolean owner, Instant now) {
        if (nation != null) {
            appearOfflineCache.put(userId, nation.isAppearOffline());
        }
        boolean hidden = !owner && isHidden(userId);
        PresenceStatus status = hidden ? PresenceStatus.OFFLINE : statusOf(userId, now);
        String lastActiveAt = null;
        if (status == PresenceStatus.OFFLINE && !hidden && nation != null && nation.getLastSeenAt() != null) {
            lastActiveAt = nation.getLastSeenAt().toString();
        }
        int solved = status == PresenceStatus.OFFLINE ? 0 : solvedSecondsLeft(userId, now);
        return new PresenceDto(status.name(), lastActiveAt, solved);
    }

    /** The visitor view (used for the SSE stream): loads the nation row for the last-seen time. */
    public PresenceDto publicState(Long userId, Instant now) {
        return forViewer(userId, nationStore.findNation(userId).orElse(null), false, now);
    }

    // ------------------------------------------------------------------ subscriptions (SSE hub)

    /** @return a function that removes the listener again. */
    public Runnable subscribe(Long userId, Consumer<PresenceDto> listener) {
        listeners.compute(userId, (id, existing) -> {
            Set<Consumer<PresenceDto>> subscribers = existing == null ? new CopyOnWriteArraySet<>() : existing;
            if (subscribers.size() >= props.getPresenceMaxStreamsPerUser()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many viewers are watching this nation");
            }
            subscribers.add(listener);
            return subscribers;
        });
        return () -> listeners.computeIfPresent(userId, (id, subscribers) -> {
            subscribers.remove(listener);
            return subscribers.isEmpty() ? null : subscribers;
        });
    }

    // ------------------------------------------------------------------ time-based changes

    @Scheduled(fixedDelay = 5_000)
    public void scheduledTick() {
        tick(Instant.now());
    }

    /**
     * Expires sessions whose heartbeat is too old, and notices time-based changes (ONLINE to IDLE after the
     * idle window, the end of the "just solved" glow, a session timing out).
     */
    public void tick(Instant now) {
        Set<Long> users = new HashSet<>(store.userIds());
        users.addAll(lastPublished.keySet());
        users.addAll(justSolvedUntil.keySet());

        for (Long userId : users) {
            expireSessions(userId, now);
            publish(userId, now);
            Instant until = justSolvedUntil.get(userId);
            if (until != null && !until.isAfter(now)) {
                justSolvedUntil.remove(userId);
            }
        }
    }

    // ------------------------------------------------------------------ internals

    private boolean isLive(PresenceStore.SessionState session, Instant now) {
        return session.lastHeartbeatAt().plusSeconds(props.getPresenceHeartbeatTtlSeconds()).isAfter(now);
    }

    /** True status from the live sessions, ignoring "Appear offline". */
    private PresenceStatus statusOf(Long userId, Instant now) {
        Duration idleAfter = Duration.ofMinutes(props.getPresenceIdleAfterMinutes());
        boolean live = false;
        boolean active = false;
        for (PresenceStore.SessionState session : store.sessions(userId).values()) {
            if (!isLive(session, now)) {
                continue;
            }
            live = true;
            if (session.lastActiveAt() != null && session.lastActiveAt().plus(idleAfter).isAfter(now)) {
                active = true;
            }
        }
        if (!live) {
            return PresenceStatus.OFFLINE;
        }
        return active ? PresenceStatus.ONLINE : PresenceStatus.IDLE;
    }

    private boolean isHidden(Long userId) {
        Boolean cached = appearOfflineCache.get(userId);
        if (cached == null) {
            cached = nationStore.findNation(userId).map(Nation::isAppearOffline).orElse(false);
            appearOfflineCache.put(userId, cached);
        }
        return cached;
    }

    private PresenceStatus publicStatus(Long userId, Instant now) {
        return isHidden(userId) ? PresenceStatus.OFFLINE : statusOf(userId, now);
    }

    private int solvedSecondsLeft(Long userId, Instant now) {
        Instant until = justSolvedUntil.get(userId);
        if (until == null || !until.isAfter(now)) {
            return 0;
        }
        long millis = Duration.between(now, until).toMillis();
        return (int) ((millis + 999) / 1000);
    }

    private void expireSessions(Long userId, Instant now) {
        Instant newestExpired = null;
        boolean anyLive = false;
        for (Map.Entry<String, PresenceStore.SessionState> entry : store.sessions(userId).entrySet()) {
            PresenceStore.SessionState session = entry.getValue();
            if (isLive(session, now)) {
                anyLive = true;
            } else {
                store.removeSession(userId, entry.getKey());
                if (newestExpired == null || session.lastHeartbeatAt().isAfter(newestExpired)) {
                    newestExpired = session.lastHeartbeatAt();
                }
            }
        }
        if (newestExpired != null && !anyLive) {
            persistLastSeen(userId, newestExpired);
        }
    }

    private void enforceSessionLimit(Long userId) {
        Map<String, PresenceStore.SessionState> sessions = store.sessions(userId);
        int excess = sessions.size() - props.getPresenceMaxSessionsPerUser();
        while (excess-- > 0) {
            String oldestId = null;
            Instant oldest = null;
            for (Map.Entry<String, PresenceStore.SessionState> entry : sessions.entrySet()) {
                Instant beat = entry.getValue().lastHeartbeatAt();
                if (oldest == null || beat.isBefore(oldest)) {
                    oldest = beat;
                    oldestId = entry.getKey();
                }
            }
            store.removeSession(userId, oldestId);
            sessions.remove(oldestId);
        }
    }

    private void maybePersistLastSeen(Long userId, Instant now) {
        Instant last = lastPersisted.get(userId);
        if (last == null || !now.isBefore(last.plusSeconds(props.getPresenceLastSeenPersistSeconds()))) {
            persistLastSeen(userId, now);
        }
    }

    /** Best effort: a database hiccup must never break presence. */
    private void persistLastSeen(Long userId, Instant at) {
        try {
            nationStore.recordLastSeen(userId, at);
            lastPersisted.put(userId, at);
        } catch (RuntimeException ignored) {
            // retried on the next heartbeat
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || !SESSION_ID.matcher(sessionId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session id");
        }
    }

    private void publish(Long userId, Instant now) {
        PresenceStatus status = publicStatus(userId, now);
        Published current = new Published(status, status != PresenceStatus.OFFLINE && solvedSecondsLeft(userId, now) > 0);
        Published previous = lastPublished.put(userId, current);
        if (current.status() == PresenceStatus.OFFLINE && !current.solving()) {
            lastPublished.remove(userId);   // keeps the map small: absent means "offline, not glowing"
        }
        Published before = previous == null ? new Published(PresenceStatus.OFFLINE, false) : previous;
        if (before.equals(current)) {
            return;
        }
        Set<Consumer<PresenceDto>> subscribers = listeners.get(userId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        PresenceDto dto = publicState(userId, now);
        for (Consumer<PresenceDto> subscriber : subscribers) {
            try {
                subscriber.accept(dto);
            } catch (RuntimeException ignored) {
                // a broken listener must not stop the others; the hub removes dead streams itself
            }
        }
    }
}
