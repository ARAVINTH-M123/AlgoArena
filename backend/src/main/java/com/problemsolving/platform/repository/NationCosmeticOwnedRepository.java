package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationCosmeticOwned;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NationCosmeticOwnedRepository extends JpaRepository<NationCosmeticOwned, Long> {

    List<NationCosmeticOwned> findByUserId(Long userId);

    boolean existsByUserIdAndCosmeticKey(Long userId, String cosmeticKey);
}
