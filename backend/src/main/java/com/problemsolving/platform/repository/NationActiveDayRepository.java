package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationActiveDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface NationActiveDayRepository extends JpaRepository<NationActiveDay, Long> {
    boolean existsByNationIdAndLocalDay(Long nationId, LocalDate localDay);
    long countByNationId(Long nationId);
}
