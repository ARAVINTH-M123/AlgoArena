package com.problemsolving.platform.domain.entity;

import com.problemsolving.platform.domain.enums.NationTrack;
import jakarta.persistence.*;

/** Build progress of one track (one row per track per nation). progress is in micro-points. */
@Entity
@Table(name = "nation_track_states", uniqueConstraints = @UniqueConstraint(columnNames = {"nation_id", "track"}))
public class NationTrackState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_id", nullable = false)
    private Long nationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "track", nullable = false, length = 20)
    private NationTrack track;

    @Column(name = "current_item_key", nullable = false, length = 80)
    private String currentItemKey;

    @Column(name = "progress", nullable = false)
    private long progress = 0L;

    @Column(name = "items_completed", nullable = false)
    private int itemsCompleted = 0;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber = 0;

    @Column(name = "era_cursor", nullable = false)
    private int eraCursor = 0;

    @Column(name = "gate_done", nullable = false)
    private boolean gateDone = false;

    public Long getId() {
        return id;
    }

    public Long getNationId() {
        return nationId;
    }

    public void setNationId(Long nationId) {
        this.nationId = nationId;
    }

    public NationTrack getTrack() {
        return track;
    }

    public void setTrack(NationTrack track) {
        this.track = track;
    }

    public String getCurrentItemKey() {
        return currentItemKey;
    }

    public void setCurrentItemKey(String currentItemKey) {
        this.currentItemKey = currentItemKey;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public int getItemsCompleted() {
        return itemsCompleted;
    }

    public void setItemsCompleted(int itemsCompleted) {
        this.itemsCompleted = itemsCompleted;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getEraCursor() {
        return eraCursor;
    }

    public void setEraCursor(int eraCursor) {
        this.eraCursor = eraCursor;
    }

    public boolean isGateDone() {
        return gateDone;
    }

    public void setGateDone(boolean gateDone) {
        this.gateDone = gateDone;
    }
}
