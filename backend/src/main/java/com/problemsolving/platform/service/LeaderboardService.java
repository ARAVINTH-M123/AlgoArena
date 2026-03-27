package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.leaderboard.LeaderboardRowDto;
import com.problemsolving.platform.domain.entity.Submission;
import com.problemsolving.platform.domain.enums.Verdict;
import com.problemsolving.platform.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LeaderboardService {

    private final SubmissionRepository submissionRepository;

    public LeaderboardService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<LeaderboardRowDto> global() {
        List<Submission> submissions = submissionRepository.findAll();
        Map<Long, ScoreAccumulator> byUser = new HashMap<>();

        for (Submission submission : submissions) {
            Long userId = submission.getUser().getId();
            ScoreAccumulator accumulator = byUser.computeIfAbsent(userId, id ->
                    new ScoreAccumulator(userId, submission.getUser().getUsername()));

            if (submission.getVerdict() == Verdict.ACCEPTED) {
                accumulator.acceptedProblemIds.add(submission.getProblem().getId());
            } else {
                accumulator.penalty += 10;
            }
        }

        return byUser.values().stream()
                .map(acc -> new LeaderboardRowDto(
                        acc.userId,
                        acc.username,
                        acc.acceptedProblemIds.size() * 100,
                        acc.penalty))
                .sorted(Comparator.comparing(LeaderboardRowDto::getScore).reversed()
                        .thenComparing(LeaderboardRowDto::getTimePenalty)
                        .thenComparing(LeaderboardRowDto::getUsername))
                .limit(100)
                .toList();
    }

    private static class ScoreAccumulator {
        private final Long userId;
        private final String username;
        private final Set<Long> acceptedProblemIds = new HashSet<>();
        private int penalty = 0;

        private ScoreAccumulator(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}
