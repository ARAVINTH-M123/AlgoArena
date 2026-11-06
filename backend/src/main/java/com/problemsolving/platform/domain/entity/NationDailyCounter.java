package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** First-solves per nation-local day: easy counting for the daily halving, first_solves for the daily goal (Phase 7). */
@Entity
@Table(name = "nation_daily_counters", uniqueConstraints = @UniqueConstraint(columnNames = {"nation_id", "local_day"}))
public class NationDailyCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_id", nullable = false)
    private Long nationId;

    @Column(name = "local_day", nullable = false)
    private LocalDate localDay;

    @Column(name = "easy_first_solves", nullable = false)
    private int easyFirstSolves = 0;

    /** All first-solves of the day (any difficulty); drives the daily-goal bonus. */
    @Column(name = "first_solves", nullable = false, columnDefinition = "int default 0")
    private int firstSolves = 0;

    public Long getId() {
        return id;
    }

    public Long getNationId() {
        return nationId;
    }

    public void setNationId(Long nationId) {
        this.nationId = nationId;
    }

    public LocalDate getLocalDay() {
        return localDay;
    }

    public void setLocalDay(LocalDate localDay) {
        this.localDay = localDay;
    }

    public int getEasyFirstSolves() {
        return easyFirstSolves;
    }

    public void setEasyFirstSolves(int easyFirstSolves) {
        this.easyFirstSolves = easyFirstSolves;
    }

    public int getFirstSolves() {
        return firstSolves;
    }

    public void setFirstSolves(int firstSolves) {
        this.firstSolves = firstSolves;
    }
}
