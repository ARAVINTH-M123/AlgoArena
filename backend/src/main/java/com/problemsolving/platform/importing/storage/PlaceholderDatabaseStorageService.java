package com.problemsolving.platform.importing.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder implementation of {@link DatabaseStorageService}.
 *
 * TODO(deployment): integrate the real database provider's storage metrics here, e.g.
 *   - Postgres: estimate from pg_database_size() vs. the disk/partition size, or read
 *     the docker/cloud volume usage metrics;
 *   - managed clouds: query the instance's max allocated storage vs. used storage.
 *
 * Behaviour until then:
 *   - getCurrentUsageMb() returns null ("unknown"): the configured max-limit/threshold
 *     check stays passive and imports proceed - insert-time storage-limit errors are
 *     still caught and stop the import with STORAGE_FULL;
 *   - isStorageLimitError(...) uses a provider-agnostic keyword scan over the error
 *     chain ("no space left on device", "tablespace full", ...). Override it with
 *     provider-specific error codes once the provider is known.
 */
@Component
public class PlaceholderDatabaseStorageService implements DatabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderDatabaseStorageService.class);

    /** Provider-agnostic markers for "the database ran out of storage". */
    private static final String[] STORAGE_ERROR_MARKERS = {
            "no space left",
            "disk full",
            "out of space",
            "out of memory or disk space",
            "tablespace full",
            "tablespace is full",
            "could not extend",
            "quota exceeded",
            "storage limit",
            "storage full"
    };

    @Override
    public Double getCurrentUsageMb() {
        // TODO(deployment): return the real database usage in MB here.
        log.debug("Database storage usage requested but no provider integration is configured - returning unknown");
        return null;
    }

    @Override
    public boolean isStorageLimitError(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 10) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                for (String marker : STORAGE_ERROR_MARKERS) {
                    if (normalized.contains(marker)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
}
