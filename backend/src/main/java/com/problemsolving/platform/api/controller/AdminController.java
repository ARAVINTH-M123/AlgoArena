package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.admin.AdminSessionResponse;
import com.problemsolving.platform.api.dto.admin.AdminUserSummary;
import com.problemsolving.platform.repository.ContestRepository;
import com.problemsolving.platform.repository.ProblemRepository;
import com.problemsolving.platform.repository.SubmissionRepository;
import com.problemsolving.platform.repository.UserRepository;
import com.problemsolving.platform.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Management console API. Every endpoint requires ROLE_ADMIN:
 * SecurityConfig gates /api/admin/** and @PreAuthorize re-checks the role per request.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final SubmissionRepository submissionRepository;
    private final AdminService adminService;

    public AdminController(UserRepository userRepository,
                           ProblemRepository problemRepository,
                           ContestRepository contestRepository,
                           SubmissionRepository submissionRepository,
                           AdminService adminService) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.contestRepository = contestRepository;
        this.submissionRepository = submissionRepository;
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "users", userRepository.count(),
                "problems", problemRepository.count(),
                "contests", contestRepository.count(),
                "submissions", submissionRepository.count());
    }

    /** Confirms to the console that the current session still belongs to a valid admin account. */
    @GetMapping("/me")
    public AdminSessionResponse me(Authentication authentication) {
        return adminService.currentAdmin(authentication.getName());
    }

    /** Platform accounts for the Users module. Never exposes password hashes. */
    @GetMapping("/users")
    public List<AdminUserSummary> users() {
        return adminService.listUsers();
    }
}
