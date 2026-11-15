package com.problemsolving.platform.importing.api;

import com.problemsolving.platform.importing.domain.ProblemImportState;

import java.time.LocalDateTime;

/** Read-only view of the problem import state for the admin API. */
public record ProblemImportStatusDto(
        String status,
        Long lastOffset,
        Long importedCount,
        LocalDateTime lastRunAt,
        LocalDateTime lastFinishedAt,
        LocalDateTime updatedAt,
        String message,
        String dataset,
        Integer batchSize,
        int importCountPerRun,
        double storageThresholdMb,
        Double storageUsageMb,
        boolean storageUsageKnown,
        String nextScheduledRunCron) {

    public static ProblemImportStatusDto from(ProblemImportState state,
                                              String dataset,
                                              int batchSize,
                                              int importCountPerRun,
                                              double storageThresholdMb,
                                              Double storageUsageMb,
                                              String cron) {
        return new ProblemImportStatusDto(
                state.getStatus() == null ? null : state.getStatus().name(),
                state.getLastOffset(),
                state.getImportedCount(),
                state.getLastRunAt(),
                state.getLastFinishedAt(),
                state.getUpdatedAt(),
                state.getMessage(),
                dataset,
                batchSize,
                importCountPerRun,
                storageThresholdMb,
                storageUsageMb,
                storageUsageMb != null,
                cron);
    }
}
