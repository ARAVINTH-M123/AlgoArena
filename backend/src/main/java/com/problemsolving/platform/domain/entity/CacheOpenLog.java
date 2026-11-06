package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One opened Mystery Cache, for bookkeeping and so the collection page can show honest history. */
@Entity
@Table(name = "cache_open_log")
public class CacheOpenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    /** Rarity that was rolled (the result before duplicate conversion). */
    @Column(name = "rarity", nullable = false, length = 20)
    private String rarity;

    /** The cosmetic the cache contained; null is never expected but kept nullable for safety. */
    @Column(name = "cosmetic_key", length = 60)
    private String cosmeticKey;

    /** Shards granted on top (non-zero only when the cosmetic was a duplicate). */
    @Column(name = "shards_gained", nullable = false)
    private long shardsGained = 0;

    /** True when the cosmetic was already owned and the cache turned into shards instead. */
    @Column(name = "was_duplicate", nullable = false, columnDefinition = "boolean default false")
    private boolean wasDuplicate = false;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getCosmeticKey() {
        return cosmeticKey;
    }

    public void setCosmeticKey(String cosmeticKey) {
        this.cosmeticKey = cosmeticKey;
    }

    public long getShardsGained() {
        return shardsGained;
    }

    public void setShardsGained(long shardsGained) {
        this.shardsGained = shardsGained;
    }

    public boolean isWasDuplicate() {
        return wasDuplicate;
    }

    public void setWasDuplicate(boolean wasDuplicate) {
        this.wasDuplicate = wasDuplicate;
    }
}
