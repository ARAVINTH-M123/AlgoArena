package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.leaderboard.LeaderboardRowDto;
import com.problemsolving.platform.domain.entity.Submission;
import com.problemsolving.platform.domain.enums.Verdict;
import com.problemsolving.platform.repository.SubmissionRepository;
import com.problemsolving.platform.service.nation.NationRankLookup;
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
    private final NationRankLookup nationRankLookup;

    public LeaderboardService(SubmissionRepository submissionRepository, NationRankLookup nationRankLookup) {
        this.submissionRepository = submissionRepository;
        this.nationRankLookup = nationRankLookup;
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

        List<ScoreAccumulator> top = byUser.values().stream()
                .sorted(Comparator.comparingInt((ScoreAccumulator acc) -> acc.acceptedProblemIds.size()).reversed()
                        .thenComparingInt(acc -> acc.penalty)
                        .thenComparing(acc -> acc.username))
                .limit(100)
                .toList();

        // nation name and rank of the users on the board (one query for all of them)
        Map<Long, String> usernames = new HashMap<>();
        for (ScoreAccumulator acc : top) {
            usernames.put(acc.userId, acc.username);
        }
        Map<Long, NationRankLookup.Info> nations = nationRankLookup.describe(usernames);

        List<LeaderboardRowDto> rows = new ArrayList<>();
        for (ScoreAccumulator acc : top) {
            NationRankLookup.Info nation = nations.get(acc.userId);
            rows.add(new LeaderboardRowDto(
                    acc.userId,
                    acc.username,
                    acc.acceptedProblemIds.size() * 100,
                    acc.penalty,
                    nation.nationName(),
                    nation.rankName(),
                    nation.rankIndex(),
                    nation.tier()));
        }
        return rows;
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
