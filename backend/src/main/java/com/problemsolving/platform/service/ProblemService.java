package com.problemsolving.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemsolving.platform.api.dto.problem.ProblemAdminResponse;
import com.problemsolving.platform.api.dto.problem.ProblemRequest;
import com.problemsolving.platform.api.dto.problem.ProblemResponse;
import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.repository.ProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;

    public ProblemService(ProblemRepository problemRepository, ObjectMapper objectMapper) {
        this.problemRepository = problemRepository;
        this.objectMapper = objectMapper;
    }

    public Page<ProblemResponse> list(String search, String difficulty) {
        List<Problem> problems;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasDifficulty = difficulty != null && !difficulty.isBlank();

        if (hasSearch && hasDifficulty) {
            problems = problemRepository.findByTitleContainingIgnoreCaseAndDifficulty(search, Difficulty.valueOf(difficulty));
        } else if (hasSearch) {
            problems = problemRepository.findByTitleContainingIgnoreCase(search);
        } else if (hasDifficulty) {
            problems = problemRepository.findByDifficulty(Difficulty.valueOf(difficulty));
        } else {
            problems = problemRepository.findAll();
        }

        List<ProblemResponse> content = problems.stream().map(this::toResponse).toList();
        return new PageImpl<>(content);
    }

    public ProblemResponse byId(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
        return toResponse(problem);
    }

    public ProblemAdminResponse byIdForAdmin(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
        return toAdminResponse(problem);
    }

    public ProblemResponse create(ProblemRequest request) {
        Problem problem = new Problem();
        applyRequest(problem, request);
        return toResponse(problemRepository.save(problem));
    }

    public ProblemResponse update(Long id, ProblemRequest request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
        applyRequest(problem, request);
        return toResponse(problemRepository.save(problem));
    }

    public void delete(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
        problemRepository.delete(problem);
    }

    private void applyRequest(Problem problem, ProblemRequest request) {
        validateHiddenTestCases(request.getHiddenTestCases());

        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setDifficulty(request.getDifficulty());
        problem.setTags(String.join(",", request.getTags()));
        problem.setInputFormat(request.getInputFormat());
        problem.setOutputFormat(request.getOutputFormat());
        problem.setConstraints(request.getConstraints());
        problem.setSampleIo(request.getSampleIo());
        problem.setHiddenTestCases(request.getHiddenTestCases());
    }

    private void validateHiddenTestCases(String raw) {
        try {
            List<TestCaseItem> testCases = objectMapper.readValue(raw, new TypeReference<List<TestCaseItem>>() {
            });

            if (testCases == null || testCases.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one testcase is required");
            }

            boolean hasInvalid = testCases.stream()
                    .anyMatch(tc -> tc == null || tc.output() == null || tc.output().trim().isEmpty());

            if (hasInvalid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each testcase must include a non-empty output");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hidden testcases must be a valid JSON array");
        }
    }

    private ProblemResponse toResponse(Problem p) {
        List<String> tags = Arrays.stream(p.getTags().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return new ProblemResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getDifficulty(),
                tags,
                p.getInputFormat(),
                p.getOutputFormat(),
                p.getConstraints(),
                p.getSampleIo());
    }

    private ProblemAdminResponse toAdminResponse(Problem p) {
        List<String> tags = Arrays.stream(p.getTags().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return new ProblemAdminResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getDifficulty(),
                tags,
                p.getInputFormat(),
                p.getOutputFormat(),
                p.getConstraints(),
                p.getHiddenTestCases());
    }

    private record TestCaseItem(String input, String output) {
    }
}
