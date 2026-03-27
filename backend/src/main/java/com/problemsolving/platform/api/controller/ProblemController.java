package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.problem.ProblemRequest;
import com.problemsolving.platform.api.dto.problem.ProblemAdminResponse;
import com.problemsolving.platform.api.dto.problem.ProblemResponse;
import com.problemsolving.platform.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public Page<ProblemResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) String difficulty) {
        return problemService.list(search, difficulty);
    }

    @GetMapping("/{id}")
    public ProblemResponse byId(@PathVariable Long id) {
        return problemService.byId(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public ProblemAdminResponse byIdForAdmin(@PathVariable Long id) {
        return problemService.byIdForAdmin(id);
    }

    @PostMapping
    public ProblemResponse create(@Valid @RequestBody ProblemRequest request) {
        return problemService.create(request);
    }

    @PutMapping("/{id}")
    public ProblemResponse update(@PathVariable Long id, @Valid @RequestBody ProblemRequest request) {
        return problemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        problemService.delete(id);
    }
}
