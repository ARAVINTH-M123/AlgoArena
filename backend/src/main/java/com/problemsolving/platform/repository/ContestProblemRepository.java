package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {
    List<ContestProblem> findByContestId(Long contestId);
}
