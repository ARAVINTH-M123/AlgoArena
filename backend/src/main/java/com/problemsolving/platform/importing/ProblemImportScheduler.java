package com.problemsolving.platform.importing;

import com.problemsolving.platform.importing.config.ProblemImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the problem import runs:
 *
 * - On application startup the FIRST import (initial-import-count problems) runs once the
 *   app is ready; a run interrupted by a previous restart resumes automatically from the
 *   last saved offset. A finished initial import is never repeated on restart.
 * - Quarterly (configurable via app.problem-import.cron) the next import-count-per-run
 *   problems are imported.
 *
 * Concurrency safety lives in ProblemImportService (atomic single-run lock), so a manual
 * admin trigger racing the schedule can never start a second job.
 */
@Component
public class ProblemImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProblemImportScheduler.class);

    private final ProblemImportProperties properties;
    private final ProblemImportService importService;

    public ProblemImportScheduler(ProblemImportProperties properties, ProblemImportService importService) {
        this.properties = properties;
        this.importService = importService;
    }

    /** Marks a run that a restart left RUNNING as FAILED, then runs the initial import if needed. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!properties.isEnabled()) {
            log.info("Problem import system is disabled (app.problem-import.enabled=false)");
            return;
        }
        importService.recoverInterruptedRun();
        if (importService.initialImportPending()) {
            log.info("Running the initial problem import (first {} problems)", properties.getInitialImportCount());
            importService.runImport("startup-initial-import", properties.getInitialImportCount());
        } else {
            log.info("Initial problem import already completed - quarterly schedule will import further problems");
        }
    }

    /**
     * Quarterly import of the next problems. Default cron: 03:00 on Jan/Apr/Jul/Oct 1st.
     * Override app.problem-import.cron to change the 3-month interval.
     */
    @Scheduled(cron = "${app.problem-import.cron:0 0 3 1 1,4,7,10 *}")
    public void importNextBatchQuarterly() {
        if (!properties.isEnabled()) {
            return;
        }
        importService.runImport("quarterly-schedule");
    }
}
