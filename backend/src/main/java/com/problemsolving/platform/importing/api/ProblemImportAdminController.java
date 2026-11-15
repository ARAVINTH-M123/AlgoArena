package com.problemsolving.platform.importing.api;

import com.problemsolving.platform.importing.ProblemImportService;
import com.problemsolving.platform.importing.config.ProblemImportProperties;
import com.problemsolving.platform.importing.domain.ProblemImportState;
import com.problemsolving.platform.importing.storage.DatabaseStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin API of the automated problem import - intentionally kept separate from the
 * normal problem-serving endpoints (/api/problems). Secured as ADMIN by the
 * /api/admin/** rule in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/problem-import")
public class ProblemImportAdminController {

    private final ProblemImportService importService;
    private final ProblemImportProperties properties;
    private final DatabaseStorageService storageService;

    public ProblemImportAdminController(ProblemImportService importService,
                                        ProblemImportProperties properties,
                                        DatabaseStorageService storageService) {
        this.importService = importService;
        this.properties = properties;
        this.storageService = storageService;
    }

    /** Current import status, progress (offset/imported count) and storage headroom info. */
    @GetMapping("/status")
    public ProblemImportStatusDto status() {
        ProblemImportState state = importService.currentState();
        return ProblemImportStatusDto.from(
                state,
                properties.getDataset(),
                properties.getBatchSize(),
                properties.getImportCountPerRun(),
                properties.getStorage().thresholdMb(),
                storageService.getCurrentUsageMb(),
                ProblemImportProperties.DEFAULT_CRON);
    }

    /**
     * Manually starts an import run (same path the scheduler uses: resumes from the last
     * saved offset, imports up to import-count-per-run problems). Returns 409 when an
     * import is already RUNNING.
     */
    @PostMapping("/trigger")
    @ResponseStatus(HttpStatus.OK)
    public ProblemImportStatusDto trigger() {
        ProblemImportService.ImportRunResult result = importService.runImport("manual-admin-trigger");
        if (!result.started()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A problem import is already RUNNING");
        }
        return ProblemImportStatusDto.from(
                result.state(),
                properties.getDataset(),
                properties.getBatchSize(),
                properties.getImportCountPerRun(),
                properties.getStorage().thresholdMb(),
                storageService.getCurrentUsageMb(),
                ProblemImportProperties.DEFAULT_CRON);
    }
}
