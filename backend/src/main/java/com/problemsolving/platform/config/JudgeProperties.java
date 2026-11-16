package com.problemsolving.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Controls access to user-supplied code execution. Keep disabled until the judge runs in a sandbox. */
@Component
@ConfigurationProperties(prefix = "app.judge")
public class JudgeProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void requireEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Code execution is unavailable while the secure judge is being configured");
        }
    }
}
