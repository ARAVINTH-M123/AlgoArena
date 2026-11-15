package com.problemsolving.platform.importing;

import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.importing.config.ProblemImportProperties;
import com.problemsolving.platform.importing.domain.ProblemImportState;
import com.problemsolving.platform.importing.domain.ProblemImportStateRepository;
import com.problemsolving.platform.importing.domain.ProblemImportStatus;
import com.problemsolving.platform.importing.hf.HfProblemRow;
import com.problemsolving.platform.importing.hf.HuggingFaceProblemClient;
import com.problemsolving.platform.importing.storage.DatabaseStorageService;
import com.problemsolving.platform.repository.ProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the automated problem import from the Hugging Face dataset.
 *
 * One run:
 *  1. atomically acquires the single-run lock (status -> RUNNING, no concurrent runs);
 *  2. checks the database storage against the configured threshold (STORAGE_FULL if reached);
 *  3. loops over batches of {@code batch-size} dataset rows starting from the last saved
 *     offset - storage is rechecked before every batch, each batch is saved in its own
 *     transaction and only then the offset is advanced, so an interrupted run resumes
 *     from the last safe position without duplicates;
 *  4. finishes with COMPLETED, STORAGE_FULL or FAILED.
 *
 * All database-provider specifics stay behind {@link DatabaseStorageService}; this
 * service contains no provider-specific storage logic.
 */
@Service
public class ProblemImportService {

    private static final Logger log = LoggerFactory.getLogger(ProblemImportService.class);

    /** The one and only import state row. */
    public static final long STATE_ID = 1L;

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final ProblemImportProperties properties;
    private final ProblemImportStateRepository stateRepository;
    private final ProblemRepository problemRepository;
    private final HuggingFaceProblemClient huggingFaceClient;
    private final DatabaseStorageService storageService;
    private final ProblemImportMapper mapper;

    /** @param started false when the trigger was ignored (disabled or another run is RUNNING). */
    public record ImportRunResult(ProblemImportState state, boolean started) {
    }

    public ProblemImportService(ProblemImportProperties properties,
                                ProblemImportStateRepository stateRepository,
                                ProblemRepository problemRepository,
                                HuggingFaceProblemClient huggingFaceClient,
                                DatabaseStorageService storageService,
                                ProblemImportMapper mapper) {
        this.properties = properties;
        this.stateRepository = stateRepository;
        this.problemRepository = problemRepository;
        this.huggingFaceClient = huggingFaceClient;
        this.storageService = storageService;
        this.mapper = mapper;
    }

    /** Current import state, creating the state row on first access. */
    public ProblemImportState currentState() {
        return ensureStateRow();
    }

    /**
     * Startup recovery: a run left RUNNING by a crash/restart can never release the lock
     * itself, so it is marked FAILED here. Offset and imported count stay untouched and
     * the next run resumes from the last saved offset. No-op when no run is active.
     */
    public void recoverInterruptedRun() {
        ensureStateRow();
        int recovered = stateRepository.failIfRunning(STATE_ID,
                ProblemImportStatus.RUNNING,
                ProblemImportStatus.FAILED,
                "Import run was interrupted by an application restart; it will resume from the last saved offset",
                LocalDateTime.now());
        if (recovered > 0) {
            log.warn("Recovered an import run that was left RUNNING by a restart - marked FAILED so the next run can resume");
        }
    }

    /** True when the initial (startup) import has not fully completed yet. */
    public boolean initialImportPending() {
        ProblemImportState state = ensureStateRow();
        return state.getStatus() != ProblemImportStatus.COMPLETED
                && state.getStatus() != ProblemImportStatus.RUNNING
                && state.getLastOffset() < properties.getInitialImportCount();
    }

    /** Imports the next batch of problems using the configured import-count-per-run target. */
    public ImportRunResult runImport(String trigger) {
        return runImport(trigger, properties.getImportCountPerRun());
    }

    /**
     * Runs one import of up to {@code targetCount} NEW problems, resuming from the last
     * persisted offset. Safe to call from anywhere: concurrent triggers are rejected via
     * the atomic single-run lock.
     */
    public ImportRunResult runImport(String trigger, int targetCount) {
        ProblemImportState state = ensureStateRow();

        if (!properties.isEnabled()) {
            log.info("Problem import is disabled - trigger '{}' ignored", trigger);
            return new ImportRunResult(state, false);
        }

        // Prevent multiple import jobs from running simultaneously (works across app instances).
        if (stateRepository.tryAcquire(STATE_ID, ProblemImportStatus.RUNNING, LocalDateTime.now()) == 0) {
            log.info("Problem import trigger '{}' ignored - another import is RUNNING", trigger);
            return new ImportRunResult(readState(state), false);
        }

        long offset = state.getLastOffset();
        long importedCount = state.getImportedCount();
        long cursor = offset;

        try {
            log.info("Problem import started (trigger={}, offset={}, importedTotal={})", trigger, offset, importedCount);

            // Requirement: check the storage before starting an import.
            String storageLimitMessage = storageLimitMessageIfReached();
            if (storageLimitMessage != null) {
                finish(ProblemImportStatus.STORAGE_FULL, storageLimitMessage + " - import stopped at dataset offset " + cursor,
                        cursor, importedCount);
                return new ImportRunResult(readState(state), true);
            }

            int savedThisRun = 0;
            while (savedThisRun < targetCount) {
                int fetchLimit = Math.min(properties.getBatchSize(), targetCount - savedThisRun);
                List<HfProblemRow> rows = huggingFaceClient.fetchRows(cursor, fetchLimit);

                if (rows.isEmpty()) {
                    finish(ProblemImportStatus.COMPLETED, "Dataset exhausted: no further problems to import", cursor, importedCount);
                    return new ImportRunResult(readState(state), true);
                }

                List<Problem> batch = prepareBatch(rows);

                // Requirement: recheck the storage before every batch of problems.
                storageLimitMessage = storageLimitMessageIfReached();
                if (storageLimitMessage != null) {
                    finish(ProblemImportStatus.STORAGE_FULL, storageLimitMessage + " - import stopped at dataset offset " + cursor,
                            cursor, importedCount);
                    return new ImportRunResult(readState(state), true);
                }

                try {
                    if (!batch.isEmpty()) {
                        // One transaction per batch: committed progress is never lost.
                        problemRepository.saveAll(batch);
                    }
                } catch (DataAccessException ex) {
                    // A storage-limit error while inserting must surface as STORAGE_FULL, not FAILED.
                    if (storageService.isStorageLimitError(ex)) {
                        log.warn("Database storage limit reached while saving a batch", ex);
                        finish(ProblemImportStatus.STORAGE_FULL,
                                "Database storage limit reached while inserting a batch: " + rootMessage(ex) + " - import stopped at dataset offset " + cursor,
                                cursor, importedCount);
                    } else {
                        log.error("Database error while saving an import batch", ex);
                        finish(ProblemImportStatus.FAILED,
                                "Database error while inserting a batch: " + rootMessage(ex) + " - import stopped at dataset offset " + cursor,
                                cursor, importedCount);
                    }
                    return new ImportRunResult(readState(state), true);
                }

                cursor += rows.size();
                importedCount += batch.size();
                savedThisRun += batch.size();
                // Requirement: update the offset only after the batch was saved successfully.
                stateRepository.updateProgress(STATE_ID, cursor, importedCount, LocalDateTime.now());
                log.info("Import batch saved: batchOffset={}, savedThisRun={}, importedTotal={}", cursor, savedThisRun, importedCount);
            }

            finish(ProblemImportStatus.COMPLETED, "Imported " + savedThisRun + " problems (trigger: " + trigger + ")", cursor, importedCount);
            return new ImportRunResult(readState(state), true);
        } catch (Exception ex) {
            // Covers Hugging Face API failures and anything unexpected: stop safely and keep the offset.
            log.error("Problem import failed at offset {}", cursor, ex);
            finish(ProblemImportStatus.FAILED,
                    "Import failed at dataset offset " + cursor + ": " + rootMessage(ex),
                    cursor, importedCount);
            return new ImportRunResult(readState(state), true);
        }
    }

    /**
     * Maps one fetched page into savable problems: skips rows that cannot be served and
     * drops problems already present in the database (duplicate prevention, also protects
     * against dataset reordering between runs).
     */
    private List<Problem> prepareBatch(List<HfProblemRow> rows) {
        List<Problem> mapped = new ArrayList<>();
        Set<String> seenExternalIds = new HashSet<>();

        for (HfProblemRow row : rows) {
            Problem problem;
            try {
                problem = mapper.mapToProblem(row);
            } catch (Exception ex) {
                log.warn("Skipping dataset row {} that could not be mapped: {}", row == null ? "?" : row.pid(), ex.getMessage());
                continue;
            }
            if (problem == null || !seenExternalIds.add(problem.getExternalId())) {
                continue;
            }
            mapped.add(problem);
        }

        if (mapped.isEmpty()) {
            return mapped;
        }

        List<String> externalIds = mapped.stream().map(Problem::getExternalId).toList();
        Set<String> existing = new HashSet<>(problemRepository.findExistingExternalIds(externalIds));
        if (existing.isEmpty()) {
            return mapped;
        }
        mapped.removeIf(problem -> existing.contains(problem.getExternalId()));
        log.info("Skipping {} already imported problem(s) in this batch", existing.size());
        return mapped;
    }

    /**
     * @return a human-readable message when the configured storage threshold is reached,
     *         null otherwise. Null usage ("unknown", e.g. the placeholder storage service)
     *         never stops the import.
     */
    private String storageLimitMessageIfReached() {
        Double usageMb = storageService.getCurrentUsageMb();
        if (usageMb == null) {
            return null;
        }
        double thresholdMb = properties.getStorage().thresholdMb();
        if (usageMb >= thresholdMb) {
            log.warn("Database storage usage {} MB reached the import threshold {} MB", usageMb, thresholdMb);
            return "Database storage usage " + usageMb + " MB reached the configured threshold " + thresholdMb + " MB";
        }
        return null;
    }

    private ProblemImportState ensureStateRow() {
        return stateRepository.findById(STATE_ID)
                .orElseGet(this::createStateRow);
    }

    private ProblemImportState createStateRow() {
        ProblemImportState state = new ProblemImportState();
        state.setId(STATE_ID);
        state.setLastOffset(0);
        state.setImportedCount(0);
        state.setUpdatedAt(LocalDateTime.now());
        try {
            return stateRepository.saveAndFlush(state);
        } catch (DataIntegrityViolationException concurrentCreation) {
            // Another thread/instance created the row at the same time.
            return stateRepository.findById(STATE_ID).orElseThrow(() -> concurrentCreation);
        }
    }

    private ProblemImportState readState(ProblemImportState fallback) {
        return stateRepository.findById(STATE_ID).orElse(fallback);
    }

    private void finish(ProblemImportStatus status, String message, long offset, long importedCount) {
        stateRepository.finish(STATE_ID, status, truncateMessage(message), offset, importedCount, LocalDateTime.now());
        log.info("Problem import finished: status={}, offset={}, importedTotal={}", status, offset, importedCount);
    }

    private static String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 10) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
            current = current.getCause();
            depth++;
        }
        return error == null ? "unknown error" : error.getClass().getSimpleName();
    }
}
