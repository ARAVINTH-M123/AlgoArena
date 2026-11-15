package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.domain.enums.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByTitleContainingIgnoreCase(String search);
    List<Problem> findByDifficulty(Difficulty difficulty);
    List<Problem> findByTitleContainingIgnoreCaseAndDifficulty(String search, Difficulty difficulty);

    @Query("select problem.externalId from Problem problem where problem.externalId in :externalIds")
    List<String> findExistingExternalIds(@Param("externalIds") List<String> externalIds);
}
