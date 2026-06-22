package com.college.placement.shared.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private int batchSize = 10;
    private long fixedDelay = 5_000;
    private long initialDelay = 5_000;
    private int maxRetries = 5;
    private int[] retryBackoffMinutes = {1, 5, 15, 30, 60};

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public long getFixedDelay() { return fixedDelay; }
    public void setFixedDelay(long fixedDelay) { this.fixedDelay = fixedDelay; }

    public long getInitialDelay() { return initialDelay; }
    public void setInitialDelay(long initialDelay) { this.initialDelay = initialDelay; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int[] getRetryBackoffMinutes() { return retryBackoffMinutes; }
    public void setRetryBackoffMinutes(int[] retryBackoffMinutes) {
        this.retryBackoffMinutes = retryBackoffMinutes;
    }
}
