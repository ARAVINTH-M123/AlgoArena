package com.problemsolving.platform.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** A local day on which the user earned at least one first-solve. */
@Entity
@Table(name = "nation_active_days", uniqueConstraints = @UniqueConstraint(columnNames = {"nation_id", "local_day"}))
public class NationActiveDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_id", nullable = false)
    private Long nationId;

    @Column(name = "local_day", nullable = false)
    private LocalDate localDay;

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
}
