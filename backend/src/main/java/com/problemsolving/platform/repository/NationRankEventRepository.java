package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationRankEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NationRankEventRepository extends JpaRepository<NationRankEvent, Long> {
}
