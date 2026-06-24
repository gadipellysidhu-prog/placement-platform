package com.college.placement.shared.eventbus.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Centralized Micrometer metrics for the internal event bus.
 * <p>
 * Tracks: {@code eventbus.events.published}, {@code eventbus.events.handled}, {@code eventbus.events.failed}.
 * Tags: {@code eventType}, {@code handler}.
 * Exposed through Spring Boot Actuator / Prometheus scrape.
 */
@Component
public class EventBusMetrics {

    public static final String METRIC_PUBLISHED = "eventbus.events.published";
    public static final String METRIC_HANDLED   = "eventbus.events.handled";
    public static final String METRIC_FAILED    = "eventbus.events.failed";

    private final MeterRegistry meterRegistry;

    public EventBusMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPublished(String eventType) {
        meterRegistry.counter(METRIC_PUBLISHED, "eventType", eventType).increment();
    }

    public void recordHandled(String eventType, String handler) {
        meterRegistry.counter(METRIC_HANDLED, "eventType", eventType, "handler", handler).increment();
    }

    public void recordFailed(String eventType, String handler) {
        meterRegistry.counter(METRIC_FAILED, "eventType", eventType, "handler", handler).increment();
    }
}
