package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.execution.ExecutionRequest;
import com.problemsolving.platform.api.dto.execution.ExecutionResponse;
import com.problemsolving.platform.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/test")
    public ExecutionResponse executeTest(@Valid @RequestBody ExecutionRequest request) {
        return executionService.executeCustomTest(request);
    }
}
