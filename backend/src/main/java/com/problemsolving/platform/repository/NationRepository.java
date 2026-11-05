package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.Nation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NationRepository extends JpaRepository<Nation, Long> {

    Optional<Nation> findByUserId(Long userId);

    List<Nation> findByUserIdIn(Collection<Long> userIds);

    /** Row lock so two simultaneous solves by the same user are applied one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Nation n where n.userId = :userId")
    Optional<Nation> findByUserIdForUpdate(@Param("userId") Long userId);
}
