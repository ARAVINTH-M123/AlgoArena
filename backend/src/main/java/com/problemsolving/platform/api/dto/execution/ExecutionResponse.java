package com.problemsolving.platform.api.dto.execution;

public class ExecutionResponse {

    private String output;
    private String error;
    private Long executionTimeMs;

    public ExecutionResponse(String output, String error, Long executionTimeMs) {
        this.output = output;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
