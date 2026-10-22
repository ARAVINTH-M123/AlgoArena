package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.Difficulty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nation_problem_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nation_problem_progress_nation_problem", columnNames = {"nation_id", "problem_id"})
})
public class NationProblemProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nation_id", nullable = false)
    private Nation nation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    @Column(name = "xp_awarded", nullable = false)
    private Integer xpAwarded;

    @Column(name = "basics_points_awarded", nullable = false)
    private Integer basicsPointsAwarded;

    @Column(name = "structures_points_awarded", nullable = false)
    private Integer structuresPointsAwarded;

    @Column(name = "landmarks_points_awarded", nullable = false)
    private Integer landmarksPointsAwarded;

    public Long getId() {
        return id;
    }

    public Nation getNation() {
        return nation;
    }

    public void setNation(Nation nation) {
        this.nation = nation;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

    public Integer getXpAwarded() {
        return xpAwarded;
    }

    public void setXpAwarded(Integer xpAwarded) {
        this.xpAwarded = xpAwarded;
    }

    public Integer getBasicsPointsAwarded() {
        return basicsPointsAwarded;
    }

    public void setBasicsPointsAwarded(Integer basicsPointsAwarded) {
        this.basicsPointsAwarded = basicsPointsAwarded;
    }

    public Integer getStructuresPointsAwarded() {
        return structuresPointsAwarded;
    }

    public void setStructuresPointsAwarded(Integer structuresPointsAwarded) {
        this.structuresPointsAwarded = structuresPointsAwarded;
    }

    public Integer getLandmarksPointsAwarded() {
        return landmarksPointsAwarded;
    }

    public void setLandmarksPointsAwarded(Integer landmarksPointsAwarded) {
        this.landmarksPointsAwarded = landmarksPointsAwarded;
    }
}
