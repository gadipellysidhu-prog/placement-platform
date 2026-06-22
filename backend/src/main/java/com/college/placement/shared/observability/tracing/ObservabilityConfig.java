package com.college.placement.shared.observability.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Spring Boot 3's Observation API for future distributed tracing readiness.
 *
 * ObservationRegistry is auto-configured by Spring Boot via micrometer-observation.
 * ObservedAspect enables @Observed annotation support on methods.
 *
 * Future: add io.micrometer:micrometer-tracing-bridge-otel and
 *         io.opentelemetry:opentelemetry-exporter-otlp to wire OTEL exporters here.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
