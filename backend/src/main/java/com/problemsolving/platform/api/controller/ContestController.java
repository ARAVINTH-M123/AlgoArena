package com.problemsolving.platform.api.controller;

import com.problemsolving.platform.api.dto.contest.ContestRequest;
import com.problemsolving.platform.api.dto.contest.ContestResponse;
import com.problemsolving.platform.service.ContestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    public List<ContestResponse> list() {
        return contestService.list();
    }

    @GetMapping("/{id}")
    public ContestResponse byId(@PathVariable Long id) {
        return contestService.byId(id);
    }

    @PostMapping
    public ContestResponse create(@Valid @RequestBody ContestRequest request) {
        return contestService.create(request);
    }

    @GetMapping("/{id}/problems")
    public List<Long> problemIds(@PathVariable Long id) {
        return contestService.problemIds(id);
    }
}
