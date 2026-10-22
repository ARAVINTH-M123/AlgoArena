package com.problemsolving.platform.repository;

import com.problemsolving.platform.domain.entity.NationCatalogItem;
import com.problemsolving.platform.domain.enums.NationTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NationCatalogItemRepository extends JpaRepository<NationCatalogItem, Long> {
    Optional<NationCatalogItem> findByEraAndTrackAndItemOrder(Integer era, NationTrack track, Integer itemOrder);

    List<NationCatalogItem> findByEraOrderByTrackAscItemOrderAsc(Integer era);
}
