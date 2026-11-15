package com.problemsolving.platform.importing.domain;

/**
 * Status of the automated problem import job (single job row in problem_import_state).
 *
 * RUNNING       - a run currently holds the import lock.
 * COMPLETED     - the last run finished successfully (target reached or dataset exhausted).
 * STORAGE_FULL  - the run stopped because the database storage threshold was reached
 *                 (detected before a batch or via a storage-limit error while inserting).
 * FAILED        - the run stopped because of an unexpected error (Hugging Face API failure,
 *                 database error, ...).
 *
 * The offset persisted in problem_import_state is only advanced after a batch is saved
 * successfully, so a run in any terminal state always resumes exactly where it stopped.
 */
public enum ProblemImportStatus {
    RUNNING,
    COMPLETED,
    STORAGE_FULL,
    FAILED
}
