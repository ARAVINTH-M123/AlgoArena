package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.contest.ContestRequest;
import com.problemsolving.platform.api.dto.contest.ContestResponse;
import com.problemsolving.platform.domain.entity.Contest;
import com.problemsolving.platform.repository.ContestProblemRepository;
import com.problemsolving.platform.repository.ContestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;

    public ContestService(ContestRepository contestRepository, ContestProblemRepository contestProblemRepository) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
    }

    public List<ContestResponse> list() {
        return contestRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ContestResponse byId(Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contest not found"));
        return toResponse(contest);
    }

    public ContestResponse create(ContestRequest request) {
        Contest contest = new Contest();
        contest.setName(request.getName());
        contest.setDescription(request.getDescription());
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        return toResponse(contestRepository.save(contest));
    }

    public List<Long> problemIds(Long contestId) {
        return contestProblemRepository.findByContestId(contestId)
                .stream()
                .map(cp -> cp.getProblem().getId())
                .toList();
    }

    private ContestResponse toResponse(Contest c) {
        return new ContestResponse(c.getId(), c.getName(), c.getDescription(), c.getStartTime(), c.getEndTime());
    }
}
