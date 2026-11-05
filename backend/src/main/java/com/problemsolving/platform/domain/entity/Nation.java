package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One personal nation per user. xp is stored in micro-points (1 point = 1,000,000). */
@Entity
@Table(name = "nations", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
public class Nation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "xp", nullable = false)
    private long xp = 0L;

    @Column(name = "rank_index", nullable = false)
    private int rankIndex = 0;

    @Column(name = "easy_solves", nullable = false)
    private int easySolves = 0;

    @Column(name = "medium_solves", nullable = false)
    private int mediumSolves = 0;

    @Column(name = "hard_solves", nullable = false)
    private int hardSolves = 0;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "UTC";

    @Column(name = "last_time_zone_change_at")
    private Instant lastTimeZoneChangeAt;

    @Column(name = "last_rename_at")
    private Instant lastRenameAt;

    @Column(name = "last_solve_at")
    private Instant lastSolveAt;

    @Column(name = "ascended", nullable = false)
    private boolean ascended = false;

    @Column(name = "appear_offline", nullable = false, columnDefinition = "boolean default false")
    private boolean appearOffline = false;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "equipped_theme_key", length = 60)
    private String equippedThemeKey;

    @Column(name = "equipped_border_key", length = 60)
    private String equippedBorderKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public int getRankIndex() {
        return rankIndex;
    }

    public void setRankIndex(int rankIndex) {
        this.rankIndex = rankIndex;
    }

    public int getEasySolves() {
        return easySolves;
    }

    public void setEasySolves(int easySolves) {
        this.easySolves = easySolves;
    }

    public int getMediumSolves() {
        return mediumSolves;
    }

    public void setMediumSolves(int mediumSolves) {
        this.mediumSolves = mediumSolves;
    }

    public int getHardSolves() {
        return hardSolves;
    }

    public void setHardSolves(int hardSolves) {
        this.hardSolves = hardSolves;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Instant getLastTimeZoneChangeAt() {
        return lastTimeZoneChangeAt;
    }

    public void setLastTimeZoneChangeAt(Instant lastTimeZoneChangeAt) {
        this.lastTimeZoneChangeAt = lastTimeZoneChangeAt;
    }

    public Instant getLastRenameAt() {
        return lastRenameAt;
    }

    public void setLastRenameAt(Instant lastRenameAt) {
        this.lastRenameAt = lastRenameAt;
    }

    public Instant getLastSolveAt() {
        return lastSolveAt;
    }

    public void setLastSolveAt(Instant lastSolveAt) {
        this.lastSolveAt = lastSolveAt;
    }

    public boolean isAscended() {
        return ascended;
    }

    public void setAscended(boolean ascended) {
        this.ascended = ascended;
    }

    public boolean isAppearOffline() {
        return appearOffline;
    }

    public void setAppearOffline(boolean appearOffline) {
        this.appearOffline = appearOffline;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getEquippedThemeKey() {
        return equippedThemeKey;
    }

    public void setEquippedThemeKey(String equippedThemeKey) {
        this.equippedThemeKey = equippedThemeKey;
    }

    public String getEquippedBorderKey() {
        return equippedBorderKey;
    }

    public void setEquippedBorderKey(String equippedBorderKey) {
        this.equippedBorderKey = equippedBorderKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
