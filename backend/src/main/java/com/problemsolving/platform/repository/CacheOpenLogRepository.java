package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.CacheOpenLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CacheOpenLogRepository extends JpaRepository<CacheOpenLog, Long> {
}
