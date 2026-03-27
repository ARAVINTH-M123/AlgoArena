package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    List<LeaderboardEntry> findTop100ByContestIsNullOrderByScoreDescTimePenaltyAsc();
}
