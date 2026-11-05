package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** History of rank-ups. */
@Entity
@Table(name = "nation_rank_events")
public class NationRankEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_id", nullable = false)
    private Long nationId;

    @Column(name = "rank_index", nullable = false)
    private int rankIndex;

    @Column(name = "reached_at", nullable = false)
    private Instant reachedAt;

    public Long getId() {
        return id;
    }

    public Long getNationId() {
        return nationId;
    }

    public void setNationId(Long nationId) {
        this.nationId = nationId;
    }

    public int getRankIndex() {
        return rankIndex;
    }

    public void setRankIndex(int rankIndex) {
        this.rankIndex = rankIndex;
    }

    public Instant getReachedAt() {
        return reachedAt;
    }

    public void setReachedAt(Instant reachedAt) {
        this.reachedAt = reachedAt;
    }
}
