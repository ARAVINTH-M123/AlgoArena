package com.problemsolving.platform.importing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persistence for the single problem-import state row. All updates are targeted
 * @Modifying queries that commit in their own transaction, so progress stays
 * consistent even if the JVM dies mid-run.
 */
public interface ProblemImportStateRepository extends JpaRepository<ProblemImportState, Long> {

    /**
     * Atomic run lock: flips the row to RUNNING only when no run is active
     * (status is null or any terminal status). Returns 1 when the caller owns
     * the lock, 0 when another run holds it - this prevents two import jobs
     * from running at the same time (also across app instances sharing the DB).
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ProblemImportState s
            SET s.status = :status, s.lastRunAt = :now, s.updatedAt = :now
            WHERE s.id = :id AND (s.status IS NULL OR s.status <> :status)
            """)
    int tryAcquire(@Param("id") Long id,
                   @Param("status") ProblemImportStatus status,
                   @Param("now") LocalDateTime now);

    /** Advances the progress markers; called only after a batch was saved successfully. */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ProblemImportState s
            SET s.lastOffset = :offset, s.importedCount = :importedCount, s.updatedAt = :now
            WHERE s.id = :id
            """)
    int updateProgress(@Param("id") Long id,
                       @Param("offset") long offset,
                       @Param("importedCount") long importedCount,
                       @Param("now") LocalDateTime now);

    /** Moves a RUNNING run into a terminal state (COMPLETED / STORAGE_FULL / FAILED). */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ProblemImportState s
            SET s.status = :status, s.message = :message, s.lastOffset = :offset, s.importedCount = :importedCount,
                s.lastFinishedAt = :now, s.updatedAt = :now
            WHERE s.id = :id
            """)
    int finish(@Param("id") Long id,
               @Param("status") ProblemImportStatus status,
               @Param("message") String message,
               @Param("offset") long offset,
               @Param("importedCount") long importedCount,
               @Param("now") LocalDateTime now);

    /**
     * Startup recovery: a run left in RUNNING by an application crash/restart can never
     * release its lock by itself, so it is marked FAILED (progress/offset stay untouched
     * and the next run resumes from there). Assumes a single app instance per database -
     * call it only on application startup.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ProblemImportState s
            SET s.status = :failed, s.message = :message, s.lastFinishedAt = :now, s.updatedAt = :now
            WHERE s.id = :id AND s.status = :running
            """)
    int failIfRunning(@Param("id") Long id,
                      @Param("running") ProblemImportStatus running,
                      @Param("failed") ProblemImportStatus failed,
                      @Param("message") String message,
                      @Param("now") LocalDateTime now);
}
