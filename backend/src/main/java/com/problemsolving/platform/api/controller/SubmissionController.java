package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.submission.SubmissionRequest;
import com.problemsolving.platform.api.dto.submission.SubmissionResponse;
import com.problemsolving.platform.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public SubmissionResponse submit(Authentication authentication, @Valid @RequestBody SubmissionRequest request) {
        return submissionService.submit(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<SubmissionResponse> mine(Authentication authentication) {
        return submissionService.mySubmissions(authentication.getName());
    }
}
