package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationProblemProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NationProblemProgressRepository extends JpaRepository<NationProblemProgress, Long> {
    boolean existsByNationIdAndProblemId(Long nationId, Long problemId);
}
