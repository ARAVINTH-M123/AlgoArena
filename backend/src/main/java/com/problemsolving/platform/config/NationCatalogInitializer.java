package com.problemsolving.platform.config;

import com.problemsolving.platform.domain.entity.NationCatalogItem;
import com.problemsolving.platform.domain.enums.NationRank;
import com.problemsolving.platform.domain.enums.NationTrack;
import com.problemsolving.platform.repository.NationCatalogItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NationCatalogInitializer implements ApplicationRunner {

    private final NationCatalogItemRepository nationCatalogItemRepository;
    private final NationProperties nationProperties;

    public NationCatalogInitializer(NationCatalogItemRepository nationCatalogItemRepository,
                                    NationProperties nationProperties) {
        this.nationCatalogItemRepository = nationCatalogItemRepository;
        this.nationProperties = nationProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (nationCatalogItemRepository.count() > 0) {
            return;
        }

        List<NationCatalogItem> seeded = new ArrayList<>();
        for (NationProperties.CatalogItemDefinition definition : nationProperties.getCatalog()) {
            NationTrack track = definition.track();
            NationRank rank = NationRank.values()[Math.max(0, definition.era() - 1)];
            NationProperties.TrackCost cost = nationProperties.getTrackCostsByRank().get(rank);
            int itemCost = switch (track) {
                case BASICS -> cost.basics();
                case STRUCTURES -> cost.structures();
                case LANDMARKS -> cost.landmarks();
            };

            NationCatalogItem item = new NationCatalogItem();
            item.setEra(definition.era());
            item.setTrack(track);
            item.setName(definition.name());
            item.setItemOrder(definition.itemOrder());
            item.setCost(itemCost);
            seeded.add(item);
        }

        nationCatalogItemRepository.saveAll(seeded);
    }
}
