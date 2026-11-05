package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.presence.HeartbeatRequest;
import com.problemsolving.platform.api.dto.presence.OfflineRequest;
import com.problemsolving.platform.service.presence.PresenceService;
import com.problemsolving.platform.service.presence.PresenceStreamHub;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class PresenceController {

    private final PresenceService presenceService;
    private final PresenceStreamHub streamHub;

    public PresenceController(PresenceService presenceService, PresenceStreamHub streamHub) {
        this.presenceService = presenceService;
        this.streamHub = streamHub;
    }

    /** Sent every ~30 s by every open tab of a logged-in user. */
    @PostMapping("/presence/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(Authentication authentication, @RequestBody HeartbeatRequest request) {
        boolean accepted = presenceService.heartbeatByUsername(
                authentication.getName(), request.getSessionId(), request.isActive(), Instant.now());
        if (!accepted) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Heartbeat sent too soon");
        }
    }

    /** Sent when a tab closes, for fast offline detection. */
    @PostMapping("/presence/offline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void offline(Authentication authentication, @RequestBody OfflineRequest request) {
        presenceService.offlineByUsername(authentication.getName(), request.getSessionId(), Instant.now());
    }

    /**
     * Public live stream of a user's presence (like profiles, anyone may watch). Browsers' EventSource cannot
     * send an Authorization header, so visitors always get the visitor view ("Appear offline" respected).
     */
    @GetMapping(value = "/nation/{username}/presence/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable String username, HttpServletRequest request) {
        Long userId = presenceService.resolveUserId(username);
        return streamHub.subscribe(userId, request.getRemoteAddr());
    }
}
