package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

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

    @Column(name = "equipped_anthem_key", length = 60)
    private String equippedAnthemKey;

    // ---- flag (Phase 5B): all nullable; a null pattern means "never customized" -> the default flag of the rank
    @Column(name = "flag_pattern", length = 40)
    private String flagPattern;

    /** Comma-separated color keys, in the order the owner picked them (2 or 3 entries). */
    @Column(name = "flag_colors", length = 160)
    private String flagColors;

    /** Null when the owner chose no emblem. */
    @Column(name = "flag_emblem", length = 60)
    private String flagEmblem;

    // ---- caches and shards (Phase 6): counters get DB defaults so existing rows migrate safely
    /** Mystery Caches waiting to be opened. */
    @Column(name = "unopened_caches", nullable = false, columnDefinition = "int default 0")
    private int unopenedCaches = 0;

    /** Accepted FIRST solves since the last cache dropped (pity counter for the 40-solve guarantee). */
    @Column(name = "solves_since_cache", nullable = false, columnDefinition = "int default 0")
    private int solvesSinceCache = 0;

    /** Opens without a Rare-or-better result (pity counter for the every-10th guarantee). */
    @Column(name = "opens_since_rare", nullable = false, columnDefinition = "int default 0")
    private int opensSinceRare = 0;

    /** Shards from duplicate cosmetics; crafting spends them. */
    @Column(name = "shards", nullable = false, columnDefinition = "bigint default 0")
    private long shards = 0;

    // ---- scene cosmetics (Phase 6): all nullable; null means the rank's default look
    @Column(name = "equipped_sky_key", length = 60)
    private String equippedSkyKey;

    @Column(name = "equipped_aura_key", length = 60)
    private String equippedAuraKey;

    @Column(name = "equipped_ambient_key", length = 60)
    private String equippedAmbientKey;

    // ---- retention (Phase 7): counters get DB defaults, last_streak_day is nullable, so existing rows migrate safely
    /** Consecutive nation-local days with at least one first-solve. */
    @Column(name = "streak_days", nullable = false, columnDefinition = "int default 0")
    private int streakDays = 0;

    /** Longest streak ever reached. */
    @Column(name = "best_streak_days", nullable = false, columnDefinition = "int default 0")
    private int bestStreakDays = 0;

    /** The last nation-local day that extended the streak (null: no streak yet). */
    @Column(name = "last_streak_day")
    private LocalDate lastStreakDay;

    /** Largest streak milestone granted in the CURRENT streak cycle (reset when the streak breaks). */
    @Column(name = "last_streak_milestone", nullable = false, columnDefinition = "int default 0")
    private int lastStreakMilestone = 0;

    /** Largest lifetime first-solve milestone granted so far (never reset; catch-up on the next first-solve). */
    @Column(name = "last_lifetime_milestone", nullable = false, columnDefinition = "int default 0")
    private int lastLifetimeMilestone = 0;

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

    public String getEquippedAnthemKey() {
        return equippedAnthemKey;
    }

    public void setEquippedAnthemKey(String equippedAnthemKey) {
        this.equippedAnthemKey = equippedAnthemKey;
    }

    public String getFlagPattern() {
        return flagPattern;
    }

    public void setFlagPattern(String flagPattern) {
        this.flagPattern = flagPattern;
    }

    public String getFlagColors() {
        return flagColors;
    }

    public void setFlagColors(String flagColors) {
        this.flagColors = flagColors;
    }

    public String getFlagEmblem() {
        return flagEmblem;
    }

    public void setFlagEmblem(String flagEmblem) {
        this.flagEmblem = flagEmblem;
    }

    public int getUnopenedCaches() {
        return unopenedCaches;
    }

    public void setUnopenedCaches(int unopenedCaches) {
        this.unopenedCaches = unopenedCaches;
    }

    public int getSolvesSinceCache() {
        return solvesSinceCache;
    }

    public void setSolvesSinceCache(int solvesSinceCache) {
        this.solvesSinceCache = solvesSinceCache;
    }

    public int getOpensSinceRare() {
        return opensSinceRare;
    }

    public void setOpensSinceRare(int opensSinceRare) {
        this.opensSinceRare = opensSinceRare;
    }

    public long getShards() {
        return shards;
    }

    public void setShards(long shards) {
        this.shards = shards;
    }

    public String getEquippedSkyKey() {
        return equippedSkyKey;
    }

    public void setEquippedSkyKey(String equippedSkyKey) {
        this.equippedSkyKey = equippedSkyKey;
    }

    public String getEquippedAuraKey() {
        return equippedAuraKey;
    }

    public void setEquippedAuraKey(String equippedAuraKey) {
        this.equippedAuraKey = equippedAuraKey;
    }

    public String getEquippedAmbientKey() {
        return equippedAmbientKey;
    }

    public void setEquippedAmbientKey(String equippedAmbientKey) {
        this.equippedAmbientKey = equippedAmbientKey;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public int getBestStreakDays() {
        return bestStreakDays;
    }

    public void setBestStreakDays(int bestStreakDays) {
        this.bestStreakDays = bestStreakDays;
    }

    public LocalDate getLastStreakDay() {
        return lastStreakDay;
    }

    public void setLastStreakDay(LocalDate lastStreakDay) {
        this.lastStreakDay = lastStreakDay;
    }

    public int getLastStreakMilestone() {
        return lastStreakMilestone;
    }

    public void setLastStreakMilestone(int lastStreakMilestone) {
        this.lastStreakMilestone = lastStreakMilestone;
    }

    public int getLastLifetimeMilestone() {
        return lastLifetimeMilestone;
    }

    public void setLastLifetimeMilestone(int lastLifetimeMilestone) {
        this.lastLifetimeMilestone = lastLifetimeMilestone;
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
