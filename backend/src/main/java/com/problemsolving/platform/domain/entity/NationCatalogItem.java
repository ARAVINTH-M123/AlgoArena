package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.NationTrack;
import jakarta.persistence.*;

@Entity
@Table(name = "nation_catalog_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nation_catalog_era_track_order", columnNames = {"era", "track", "item_order"})
})
public class NationCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer era;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NationTrack track;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer cost;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;

    public Long getId() {
        return id;
    }

    public Integer getEra() {
        return era;
    }

    public void setEra(Integer era) {
        this.era = era;
    }

    public NationTrack getTrack() {
        return track;
    }

    public void setTrack(NationTrack track) {
        this.track = track;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public Integer getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(Integer itemOrder) {
        this.itemOrder = itemOrder;
    }
}
