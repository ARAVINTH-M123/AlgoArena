package com.problemsolving.platform.api.dto.presence;

public class HeartbeatRequest {

    private String sessionId;
    private boolean active;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
