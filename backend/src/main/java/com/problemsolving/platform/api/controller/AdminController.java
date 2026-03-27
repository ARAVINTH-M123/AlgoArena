package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.repository.ContestRepository;
import com.problemsolving.platform.repository.ProblemRepository;
import com.problemsolving.platform.repository.SubmissionRepository;
import com.problemsolving.platform.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final SubmissionRepository submissionRepository;

    public AdminController(UserRepository userRepository,
                           ProblemRepository problemRepository,
                           ContestRepository contestRepository,
                           SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.contestRepository = contestRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "users", userRepository.count(),
                "problems", problemRepository.count(),
                "contests", contestRepository.count(),
                "submissions", submissionRepository.count());
    }
}
