package com.college.placement.shared.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Provider-agnostic storage metrics, tagged by the active backend so dashboards
 * can compare local vs object-store behaviour and surface failures.
 */
@Component
public class StorageMetrics {

    private final MeterRegistry registry;

    public StorageMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordStore(String provider) {
        counter("storage.store.total", provider).increment();
    }

    public void recordDelete(String provider) {
        counter("storage.delete.total", provider).increment();
    }

    public void recordFailure(String provider, String operation) {
        Counter.builder("storage.failure.total")
                .description("Total storage operation failures")
                .tag("provider", provider)
                .tag("operation", operation)
                .register(registry)
                .increment();
    }

    private Counter counter(String name, String provider) {
        return Counter.builder(name)
                .description("Storage operations")
                .tag("provider", provider)
                .register(registry);
    }
}
