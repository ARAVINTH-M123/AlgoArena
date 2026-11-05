package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationCompletedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NationCompletedItemRepository extends JpaRepository<NationCompletedItem, Long> {
    long countByNationIdAndCatalogKey(Long nationId, String catalogKey);
    List<NationCompletedItem> findByNationIdOrderByCompletedAtAsc(Long nationId);
}
