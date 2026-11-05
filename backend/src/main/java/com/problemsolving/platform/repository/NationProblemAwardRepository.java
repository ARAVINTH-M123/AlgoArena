package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationProblemAward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NationProblemAwardRepository extends JpaRepository<NationProblemAward, Long> {
    Optional<NationProblemAward> findByNationIdAndProblemId(Long nationId, Long problemId);
}
