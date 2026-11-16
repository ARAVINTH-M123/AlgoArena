package com.problemsolving.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JudgePropertiesTest {

    @Test
    void permitsExecutionWhenEnabled() {
        JudgeProperties properties = new JudgeProperties();

        assertDoesNotThrow(properties::requireEnabled);
    }

    @Test
    void rejectsExecutionWhenDisabled() {
        JudgeProperties properties = new JudgeProperties();
        properties.setEnabled(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, properties::requireEnabled);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }
}
