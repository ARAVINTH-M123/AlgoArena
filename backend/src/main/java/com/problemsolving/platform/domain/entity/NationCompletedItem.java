package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.NationTrack;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nation_completed_items")
public class NationCompletedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nation_id", nullable = false)
    private Nation nation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private NationCatalogItem catalogItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NationTrack track;

    @Column(nullable = false)
    private Integer era;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer cost;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public Long getId() {
        return id;
    }

    public Nation getNation() {
        return nation;
    }

    public void setNation(Nation nation) {
        this.nation = nation;
    }

    public NationCatalogItem getCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(NationCatalogItem catalogItem) {
        this.catalogItem = catalogItem;
    }

    public NationTrack getTrack() {
        return track;
    }

    public void setTrack(NationTrack track) {
        this.track = track;
    }

    public Integer getEra() {
        return era;
    }

    public void setEra(Integer era) {
        this.era = era;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
