package com.problemsolving.platform.importing.storage;

/**
 * Provider-independent storage metrics used by the problem import system.
 *
 * The import service only talks to this interface - it never queries the database
 * provider directly - so the actual implementation can be swapped per deployment
 * (Postgres disk usage, managed-cloud instance metrics, tablespace quotas, ...).
 */
public interface DatabaseStorageService {

    /**
     * Current database storage usage in MB, or {@code null} when the value cannot be
     * determined yet. Returning null means "unknown": the import proceeds instead of
     * guessing, and storage-full detection then relies only on
     * {@link #isStorageLimitError(Throwable)} during inserts.
     */
    Double getCurrentUsageMb();

    /**
     * True when the given error (and its cause chain) indicates the database hit its
     * storage limit while writing. The import marks such runs as STORAGE_FULL and stops
     * safely, keeping the already-committed offset.
     */
    boolean isStorageLimitError(Throwable error);
}
