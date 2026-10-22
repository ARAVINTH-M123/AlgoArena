package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationDailyEasyCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface NationDailyEasyCounterRepository extends JpaRepository<NationDailyEasyCounter, Long> {
    Optional<NationDailyEasyCounter> findByNationIdAndSolveDate(Long nationId, LocalDate solveDate);
}
