package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, Long> {
}
