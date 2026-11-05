package com.problemsolving.platform.config;

import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.domain.enums.NationTrack;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Central configuration for the Nation system. Every gameplay number lives here.
 * Switches (award-enabled, xp-multiplier, ...) can be overridden under the "nation" prefix in
 * application.yaml. Tables are written in WHOLE points and converted to micro-points on read
 * (1 point = 1,000,000 micro-points) so halving and tiny bonuses stay exact.
 */
@Component
@ConfigurationProperties(prefix = "nation")
public class NationProperties {

    public static final long MICRO = 1_000_000L;
    public static final int MAX_RANK_INDEX = 9;

    // ---------- switches (overridable in application.yaml) ----------
    /** Turn Nation awarding off entirely (keep OFF in public deployments until the judge is sandboxed). */
    private boolean awardEnabled = true;
    /** Dev speed-up for XP and track points. Must stay 1 in production. */
    private long xpMultiplier = 1;
    private int dailyEasyLimit = 5;
    private long easyOverLimitDivisor = 2;
    private int resolveCooldownHours = 24;
    private long resolveBonusDivisor = 10_000;
    private int renameCooldownDays = 7;
    private int timeZoneChangeCooldownDays = 30;
    private long singularityTierSpacingPoints = 5_000;

    // ---------- presence (blueprint section 8) ----------
    /** A session counts as live while its last heartbeat is younger than this. */
    private int presenceHeartbeatTtlSeconds = 75;
    /** No user activity for this long turns ONLINE into IDLE. */
    private int presenceIdleAfterMinutes = 5;
    /** Heartbeats from one session faster than this are rejected (HTTP 429). */
    private int presenceHeartbeatMinIntervalSeconds = 15;
    /** How long the figure glows after an accepted first-solve. */
    private int presenceJustSolvedSeconds = 60;
    /** last_seen_at is written to the database at most this often per user. */
    private int presenceLastSeenPersistSeconds = 60;
    private int presenceMaxSessionsPerUser = 20;
    private int presenceMaxStreamsPerUser = 200;
    private int presenceMaxStreamsPerClient = 8;

    // ---------- tables (whole points) ----------
    private static final String[] RANK_NAMES = {
            "Null", "Init", "Boot", "Root", "Kernel", "Stack", "Cloud", "Core", "Quantum", "Singularity"};
    private static final long[] RANK_XP = {0, 900, 4_500, 12_000, 27_000, 51_000, 87_000, 135_000, 204_000, 285_000};
    private static final int[] RANK_MIN_MEDIUMS = {0, 0, 0, 0, 100, 250, 500, 900, 1_800, 3_400};
    private static final int[] RANK_MIN_HARDS = {0, 0, 0, 0, 0, 0, 0, 50, 600, 1_700};
    private static final int[] RANK_MIN_ACTIVE_DAYS = {0, 0, 0, 0, 0, 0, 0, 200, 400, 700};

    // index = Difficulty ordinal (EASY, MEDIUM, HARD)
    private static final long[] XP_POINTS = {10, 30, 70};
    // [difficulty][track] with track order BASICS, STRUCTURES, LANDMARKS
    private static final long[][] TRACK_POINTS = {
            {10, 4, 1},
            {5, 10, 4},
            {3, 8, 10}};
    // [era - 1][track]
    private static final long[][] ITEM_COST_POINTS = {
            {50, 80, 100},
            {80, 140, 200},
            {120, 220, 350},
            {180, 320, 550},
            {250, 450, 800}};

    /** Starter list for nation-name moderation; extend as needed. Matched against the name with punctuation removed. */
    private static final String[] BLOCKED_NAME_WORDS = {
            "fuck", "shit", "bitch", "asshole", "cunt", "nigger", "nigga", "faggot", "whore", "slut"};

    // ---------- lookups (all return micro-points unless named otherwise) ----------
    public boolean containsBlockedWord(String name) {
        String squashed = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String word : BLOCKED_NAME_WORDS) {
            if (squashed.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public long xpMicro(Difficulty difficulty) {
        return XP_POINTS[difficulty.ordinal()] * MICRO;
    }

    public long trackPointsMicro(Difficulty difficulty, NationTrack track) {
        return TRACK_POINTS[difficulty.ordinal()][track.ordinal()] * MICRO;
    }

    public long itemCostMicro(int contentEra, NationTrack track) {
        return ITEM_COST_POINTS[contentEra - 1][track.ordinal()] * MICRO;
    }

    public String rankName(int rankIndex) {
        return RANK_NAMES[rankIndex];
    }

    public long rankXpMicro(int rankIndex) {
        return RANK_XP[rankIndex] * MICRO;
    }

    public int rankMinMediums(int rankIndex) {
        return RANK_MIN_MEDIUMS[rankIndex];
    }

    public int rankMinHards(int rankIndex) {
        return RANK_MIN_HARDS[rankIndex];
    }

    public int rankMinActiveDays(int rankIndex) {
        return RANK_MIN_ACTIVE_DAYS[rankIndex];
    }

    public long singularityTierSpacingMicro() {
        return singularityTierSpacingPoints * MICRO;
    }

    // ---------- getters / setters for the switches ----------
    public boolean isAwardEnabled() {
        return awardEnabled;
    }

    public void setAwardEnabled(boolean awardEnabled) {
        this.awardEnabled = awardEnabled;
    }

    public long getXpMultiplier() {
        return xpMultiplier;
    }

    public void setXpMultiplier(long xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    public int getDailyEasyLimit() {
        return dailyEasyLimit;
    }

    public void setDailyEasyLimit(int dailyEasyLimit) {
        this.dailyEasyLimit = dailyEasyLimit;
    }

    public long getEasyOverLimitDivisor() {
        return easyOverLimitDivisor;
    }

    public void setEasyOverLimitDivisor(long easyOverLimitDivisor) {
        this.easyOverLimitDivisor = easyOverLimitDivisor;
    }

    public int getResolveCooldownHours() {
        return resolveCooldownHours;
    }

    public void setResolveCooldownHours(int resolveCooldownHours) {
        this.resolveCooldownHours = resolveCooldownHours;
    }

    public long getResolveBonusDivisor() {
        return resolveBonusDivisor;
    }

    public void setResolveBonusDivisor(long resolveBonusDivisor) {
        this.resolveBonusDivisor = resolveBonusDivisor;
    }

    public int getRenameCooldownDays() {
        return renameCooldownDays;
    }

    public void setRenameCooldownDays(int renameCooldownDays) {
        this.renameCooldownDays = renameCooldownDays;
    }

    public int getTimeZoneChangeCooldownDays() {
        return timeZoneChangeCooldownDays;
    }

    public void setTimeZoneChangeCooldownDays(int timeZoneChangeCooldownDays) {
        this.timeZoneChangeCooldownDays = timeZoneChangeCooldownDays;
    }

    public long getSingularityTierSpacingPoints() {
        return singularityTierSpacingPoints;
    }

    public void setSingularityTierSpacingPoints(long singularityTierSpacingPoints) {
        this.singularityTierSpacingPoints = singularityTierSpacingPoints;
    }

    public int getPresenceHeartbeatTtlSeconds() {
        return presenceHeartbeatTtlSeconds;
    }

    public void setPresenceHeartbeatTtlSeconds(int presenceHeartbeatTtlSeconds) {
        this.presenceHeartbeatTtlSeconds = presenceHeartbeatTtlSeconds;
    }

    public int getPresenceIdleAfterMinutes() {
        return presenceIdleAfterMinutes;
    }

    public void setPresenceIdleAfterMinutes(int presenceIdleAfterMinutes) {
        this.presenceIdleAfterMinutes = presenceIdleAfterMinutes;
    }

    public int getPresenceHeartbeatMinIntervalSeconds() {
        return presenceHeartbeatMinIntervalSeconds;
    }

    public void setPresenceHeartbeatMinIntervalSeconds(int presenceHeartbeatMinIntervalSeconds) {
        this.presenceHeartbeatMinIntervalSeconds = presenceHeartbeatMinIntervalSeconds;
    }

    public int getPresenceJustSolvedSeconds() {
        return presenceJustSolvedSeconds;
    }

    public void setPresenceJustSolvedSeconds(int presenceJustSolvedSeconds) {
        this.presenceJustSolvedSeconds = presenceJustSolvedSeconds;
    }

    public int getPresenceLastSeenPersistSeconds() {
        return presenceLastSeenPersistSeconds;
    }

    public void setPresenceLastSeenPersistSeconds(int presenceLastSeenPersistSeconds) {
        this.presenceLastSeenPersistSeconds = presenceLastSeenPersistSeconds;
    }

    public int getPresenceMaxSessionsPerUser() {
        return presenceMaxSessionsPerUser;
    }

    public void setPresenceMaxSessionsPerUser(int presenceMaxSessionsPerUser) {
        this.presenceMaxSessionsPerUser = presenceMaxSessionsPerUser;
    }

    public int getPresenceMaxStreamsPerUser() {
        return presenceMaxStreamsPerUser;
    }

    public void setPresenceMaxStreamsPerUser(int presenceMaxStreamsPerUser) {
        this.presenceMaxStreamsPerUser = presenceMaxStreamsPerUser;
    }

    public int getPresenceMaxStreamsPerClient() {
        return presenceMaxStreamsPerClient;
    }

    public void setPresenceMaxStreamsPerClient(int presenceMaxStreamsPerClient) {
        this.presenceMaxStreamsPerClient = presenceMaxStreamsPerClient;
    }
}
