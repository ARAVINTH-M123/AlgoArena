package com.problemsolving.platform.service.presence;

import com.problemsolving.platform.api.dto.presence.PresenceDto;
import com.problemsolving.platform.config.NationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-Sent Events for visitors of a nation page. Sends the current state first, then only changes,
 * plus a keep-alive comment so proxies do not close idle streams. Limits how many streams one client
 * (IP) and one nation can hold open.
 */
@Component
public class PresenceStreamHub {

    private static final long STREAM_TIMEOUT_MS = 30L * 60L * 1000L;   // browsers reconnect on their own

    private final PresenceService presence;
    private final NationProperties props;
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicInteger> streamsPerClient = new ConcurrentHashMap<>();

    public PresenceStreamHub(PresenceService presence, NationProperties props) {
        this.presence = presence;
        this.props = props;
    }

    public SseEmitter subscribe(Long userId, String clientKey) {
        AtomicInteger counter = streamsPerClient.computeIfAbsent(clientKey, key -> new AtomicInteger());
        if (counter.incrementAndGet() > props.getPresenceMaxStreamsPerClient()) {
            release(clientKey);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many open presence streams");
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Runnable unsubscribe;
        try {
            unsubscribe = presence.subscribe(userId, state -> sendState(emitter, state));
        } catch (RuntimeException ex) {
            release(clientKey);
            throw ex;
        }

        emitters.add(emitter);
        AtomicBoolean cleaned = new AtomicBoolean(false);
        Runnable cleanup = () -> {
            if (cleaned.compareAndSet(false, true)) {
                unsubscribe.run();
                emitters.remove(emitter);
                release(clientKey);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(error -> cleanup.run());

        sendState(emitter, presence.publicState(userId, Instant.now()));   // current state right away
        return emitter;
    }

    @Scheduled(fixedDelay = 25_000)
    public void keepAlive() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException | RuntimeException ex) {
                emitter.complete();
            }
        }
    }

    private void sendState(SseEmitter emitter, PresenceDto state) {
        try {
            emitter.send(SseEmitter.event().name("presence").data(state, MediaType.APPLICATION_JSON));
        } catch (IOException | RuntimeException ex) {
            emitter.complete();   // triggers cleanup
        }
    }

    private void release(String clientKey) {
        AtomicInteger counter = streamsPerClient.get(clientKey);
        if (counter != null && counter.decrementAndGet() <= 0) {
            streamsPerClient.remove(clientKey, counter);
        }
    }
}
