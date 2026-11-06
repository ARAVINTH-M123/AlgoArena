package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A cosmetic this user owns through ACQUISITION (Mystery Cache drop, shard crafting, or a special grant such as
 * the Ascended Aura). Rank cosmetics are NOT stored here: they stay derived from the nation's rank, so nothing
 * has to be back-filled for existing nations.
 */
@Entity
@Table(name = "nation_cosmetic_owned",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "cosmetic_key"}))
public class NationCosmeticOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cosmetic_key", nullable = false, length = 60)
    private String cosmeticKey;

    /** Why it is owned: CACHE (a cache drop), SHARDS (crafted) or SPECIAL (a grant such as the Ascended Aura). */
    @Column(name = "acquired_via", nullable = false, length = 20)
    private String acquiredVia;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCosmeticKey() {
        return cosmeticKey;
    }

    public void setCosmeticKey(String cosmeticKey) {
        this.cosmeticKey = cosmeticKey;
    }

    public String getAcquiredVia() {
        return acquiredVia;
    }

    public void setAcquiredVia(String acquiredVia) {
        this.acquiredVia = acquiredVia;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(Instant acquiredAt) {
        this.acquiredAt = acquiredAt;
    }
}
