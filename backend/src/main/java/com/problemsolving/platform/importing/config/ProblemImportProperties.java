package com.problemsolving.platform.importing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the automated problem import system (Hugging Face dataset -> problems table).
 * Everything is overridable under the "app.problem-import" prefix in application.yaml.
 *
 * The storage block drives the configurable storage-full detection:
 * the import stops when the reported database usage (MB) reaches
 * {@code max-limit-mb * threshold-percentage / 100}.
 */
@Component
@ConfigurationProperties(prefix = "app.problem-import")
public class ProblemImportProperties {

    /** Default quarterly schedule: 03:00 on Jan/Apr/Jul/Oct 1st (every 3 months). */
    public static final String DEFAULT_CRON = "0 0 3 1 1,4,7,10 *";

    /** Master switch for the whole import system (startup import, quarterly schedule and manual trigger). */
    private boolean enabled = true;

    /** Hugging Face dataset id to import from (fetched through the datasets-server rows API, never downloaded locally). */
    private String dataset = "sigcp/hardtests_problems";
    /** Dataset config name on the Hugging Face datasets-server. */
    private String datasetConfig = "default";
    /** Dataset split to import from. */
    private String split = "train";
    /** Base URL of the Hugging Face datasets-server. */
    private String apiBaseUrl = "https://datasets-server.huggingface.co";

    /** Number of problems imported when the system is initialized (first startup). */
    private int initialImportCount = 500;
    /** Number of problems imported per scheduled run (every 3 months by default). */
    private int importCountPerRun = 500;
    /** Rows fetched and saved per database transaction. Storage is rechecked before every batch. */
    private int batchSize = 100;

    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;

    private final Storage storage = new Storage();

    /** Configurable storage-full detection. */
    public static class Storage {

        /** Maximum database storage the platform is allowed to use for problems, in MB. */
        private long maxLimitMb = 2048;
        /** Safety percentage of max-limit-mb at which the import must stop (50 -> 1024 MB by default). */
        private int thresholdPercentage = 50;

        /** Storage usage (MB) at or above which the import is marked STORAGE_FULL and stops. */
        public double thresholdMb() {
            return maxLimitMb * thresholdPercentage / 100.0;
        }

        public long getMaxLimitMb() {
            return maxLimitMb;
        }

        public void setMaxLimitMb(long maxLimitMb) {
            this.maxLimitMb = maxLimitMb;
        }

        public int getThresholdPercentage() {
            return thresholdPercentage;
        }

        public void setThresholdPercentage(int thresholdPercentage) {
            this.thresholdPercentage = thresholdPercentage;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getDatasetConfig() {
        return datasetConfig;
    }

    public void setDatasetConfig(String datasetConfig) {
        this.datasetConfig = datasetConfig;
    }

    public String getSplit() {
        return split;
    }

    public void setSplit(String split) {
        this.split = split;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public int getInitialImportCount() {
        return initialImportCount;
    }

    public void setInitialImportCount(int initialImportCount) {
        this.initialImportCount = initialImportCount;
    }

    public int getImportCountPerRun() {
        return importCountPerRun;
    }

    public void setImportCountPerRun(int importCountPerRun) {
        this.importCountPerRun = importCountPerRun;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public Storage getStorage() {
        return storage;
    }
}
