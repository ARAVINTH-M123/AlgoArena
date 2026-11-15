package com.problemsolving.platform.importing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Single-row (id = 1) progress record of the automated problem import.
 *
 * lastOffset is the number of dataset rows fully consumed by the importer
 * (fetched and either saved or deliberately skipped as duplicates/invalid).
 * It is advanced only AFTER a batch was saved successfully, so an interrupted
 * run always resumes from the last safe position without losing progress or
 * importing duplicates.
 */
@Entity
@Table(name = "problem_import_state")
public class ProblemImportState {

    /** Fixed row id; there is exactly one import state row. */
    @Id
    private Long id;

    /** Null until the first import run started. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProblemImportStatus status;

    @Column(name = "last_offset", nullable = false)
    private long lastOffset;

    /** Total number of problems imported from the dataset so far. */
    @Column(name = "imported_count", nullable = false)
    private long importedCount;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_finished_at")
    private LocalDateTime lastFinishedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Last human-readable result of a run (success note or error description), truncated to 1000 chars. */
    @Column(length = 1000)
    private String message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProblemImportStatus getStatus() {
        return status;
    }

    public void setStatus(ProblemImportStatus status) {
        this.status = status;
    }

    public long getLastOffset() {
        return lastOffset;
    }

    public void setLastOffset(long lastOffset) {
        this.lastOffset = lastOffset;
    }

    public long getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(long importedCount) {
        this.importedCount = importedCount;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public void setLastFinishedAt(LocalDateTime lastFinishedAt) {
        this.lastFinishedAt = lastFinishedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
