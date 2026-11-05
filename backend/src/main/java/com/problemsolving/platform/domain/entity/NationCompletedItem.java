package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.NationTrack;
import jakarta.persistence.*;
import java.time.Instant;

/** An item the nation has finished building. */
@Entity
@Table(name = "nation_completed_items")
public class NationCompletedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_id", nullable = false)
    private Long nationId;

    @Column(name = "catalog_key", nullable = false, length = 80)
    private String catalogKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "track", nullable = false, length = 20)
    private NationTrack track;

    @Column(name = "content_era", nullable = false)
    private int contentEra;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "instance_number", nullable = false)
    private int instanceNumber;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public Long getId() {
        return id;
    }

    public Long getNationId() {
        return nationId;
    }

    public void setNationId(Long nationId) {
        this.nationId = nationId;
    }

    public String getCatalogKey() {
        return catalogKey;
    }

    public void setCatalogKey(String catalogKey) {
        this.catalogKey = catalogKey;
    }

    public NationTrack getTrack() {
        return track;
    }

    public void setTrack(NationTrack track) {
        this.track = track;
    }

    public int getContentEra() {
        return contentEra;
    }

    public void setContentEra(int contentEra) {
        this.contentEra = contentEra;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getInstanceNumber() {
        return instanceNumber;
    }

    public void setInstanceNumber(int instanceNumber) {
        this.instanceNumber = instanceNumber;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
