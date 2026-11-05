package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationDailyCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface NationDailyCounterRepository extends JpaRepository<NationDailyCounter, Long> {
    Optional<NationDailyCounter> findByNationIdAndLocalDay(Long nationId, LocalDate localDay);
}
