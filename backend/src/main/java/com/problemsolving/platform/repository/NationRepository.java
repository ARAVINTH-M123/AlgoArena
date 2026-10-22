package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.Nation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NationRepository extends JpaRepository<Nation, Long> {
    Optional<Nation> findByUserUsername(String username);

    Optional<Nation> findByUserId(Long userId);
}
