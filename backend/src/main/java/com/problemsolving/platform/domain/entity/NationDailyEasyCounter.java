package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "nation_daily_easy_counters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nation_daily_easy_nation_date", columnNames = {"nation_id", "solve_date"})
})
public class NationDailyEasyCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nation_id", nullable = false)
    private Nation nation;

    @Column(name = "solve_date", nullable = false)
    private LocalDate solveDate;

    @Column(name = "easy_first_solves", nullable = false)
    private Integer easyFirstSolves = 0;

    public Long getId() {
        return id;
    }

    public Nation getNation() {
        return nation;
    }

    public void setNation(Nation nation) {
        this.nation = nation;
    }

    public LocalDate getSolveDate() {
        return solveDate;
    }

    public void setSolveDate(LocalDate solveDate) {
        this.solveDate = solveDate;
    }

    public Integer getEasyFirstSolves() {
        return easyFirstSolves;
    }

    public void setEasyFirstSolves(Integer easyFirstSolves) {
        this.easyFirstSolves = easyFirstSolves;
    }
}
