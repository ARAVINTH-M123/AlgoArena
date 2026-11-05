package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.Difficulty;
import jakarta.persistence.*;
import java.time.Instant;

/** Award record per (nation, problem); the unique constraint makes first-solve awards idempotent. */
@Entity
@Table(name = "nation_problem_awards", uniqueConstraints = @UniqueConstraint(columnNames = {"nation_id", "problem_id"}))
public class NationProblemAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_id", nullable = false)
    private Long nationId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "xp_awarded", nullable = false)
    private long xpAwarded = 0L;

    @Column(name = "first_awarded_at", nullable = false)
    private Instant firstAwardedAt;

    @Column(name = "last_awarded_at", nullable = false)
    private Instant lastAwardedAt;

    @Column(name = "resolve_bonus_count", nullable = false)
    private int resolveBonusCount = 0;

    public Long getId() {
        return id;
    }

    public Long getNationId() {
        return nationId;
    }

    public void setNationId(Long nationId) {
        this.nationId = nationId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public long getXpAwarded() {
        return xpAwarded;
    }

    public void setXpAwarded(long xpAwarded) {
        this.xpAwarded = xpAwarded;
    }

    public Instant getFirstAwardedAt() {
        return firstAwardedAt;
    }

    public void setFirstAwardedAt(Instant firstAwardedAt) {
        this.firstAwardedAt = firstAwardedAt;
    }

    public Instant getLastAwardedAt() {
        return lastAwardedAt;
    }

    public void setLastAwardedAt(Instant lastAwardedAt) {
        this.lastAwardedAt = lastAwardedAt;
    }

    public int getResolveBonusCount() {
        return resolveBonusCount;
    }

    public void setResolveBonusCount(int resolveBonusCount) {
        this.resolveBonusCount = resolveBonusCount;
    }
}
