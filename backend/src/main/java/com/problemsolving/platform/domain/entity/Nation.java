package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.NationRank;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nations_user_id", columnNames = "user_id")
})
public class Nation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long xp = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NationRank rank = NationRank.NULL_RANK;

    @Column(name = "medium_first_solves", nullable = false)
    private Integer mediumFirstSolves = 0;

    @Column(name = "hard_first_solves", nullable = false)
    private Integer hardFirstSolves = 0;

    @Column(name = "basics_progress", nullable = false)
    private Integer basicsProgress = 0;

    @Column(name = "structures_progress", nullable = false)
    private Integer structuresProgress = 0;

    @Column(name = "landmarks_progress", nullable = false)
    private Integer landmarksProgress = 0;

    @Column(name = "current_basics_item_order", nullable = false)
    private Integer currentBasicsItemOrder = 1;

    @Column(name = "current_structures_item_order", nullable = false)
    private Integer currentStructuresItemOrder = 1;

    @Column(name = "current_landmarks_item_order", nullable = false)
    private Integer currentLandmarksItemOrder = 1;

    @Column(name = "appear_offline", nullable = false)
    private Boolean appearOffline = false;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getXp() {
        return xp;
    }

    public void setXp(Long xp) {
        this.xp = xp;
    }

    public NationRank getRank() {
        return rank;
    }

    public void setRank(NationRank rank) {
        this.rank = rank;
    }

    public Integer getMediumFirstSolves() {
        return mediumFirstSolves;
    }

    public void setMediumFirstSolves(Integer mediumFirstSolves) {
        this.mediumFirstSolves = mediumFirstSolves;
    }

    public Integer getHardFirstSolves() {
        return hardFirstSolves;
    }

    public void setHardFirstSolves(Integer hardFirstSolves) {
        this.hardFirstSolves = hardFirstSolves;
    }

    public Integer getBasicsProgress() {
        return basicsProgress;
    }

    public void setBasicsProgress(Integer basicsProgress) {
        this.basicsProgress = basicsProgress;
    }

    public Integer getStructuresProgress() {
        return structuresProgress;
    }

    public void setStructuresProgress(Integer structuresProgress) {
        this.structuresProgress = structuresProgress;
    }

    public Integer getLandmarksProgress() {
        return landmarksProgress;
    }

    public void setLandmarksProgress(Integer landmarksProgress) {
        this.landmarksProgress = landmarksProgress;
    }

    public Integer getCurrentBasicsItemOrder() {
        return currentBasicsItemOrder;
    }

    public void setCurrentBasicsItemOrder(Integer currentBasicsItemOrder) {
        this.currentBasicsItemOrder = currentBasicsItemOrder;
    }

    public Integer getCurrentStructuresItemOrder() {
        return currentStructuresItemOrder;
    }

    public void setCurrentStructuresItemOrder(Integer currentStructuresItemOrder) {
        this.currentStructuresItemOrder = currentStructuresItemOrder;
    }

    public Integer getCurrentLandmarksItemOrder() {
        return currentLandmarksItemOrder;
    }

    public void setCurrentLandmarksItemOrder(Integer currentLandmarksItemOrder) {
        this.currentLandmarksItemOrder = currentLandmarksItemOrder;
    }

    public Boolean getAppearOffline() {
        return appearOffline;
    }

    public void setAppearOffline(Boolean appearOffline) {
        this.appearOffline = appearOffline;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
