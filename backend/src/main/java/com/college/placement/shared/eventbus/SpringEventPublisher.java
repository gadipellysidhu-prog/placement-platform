package com.college.placement.shared.eventbus;

import com.college.placement.shared.eventbus.metrics.EventBusMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Delegates to Spring's ApplicationEventPublisher.
 * Validates event metadata before dispatch and emits structured log + Micrometer metric.
 */
@Slf4j
@Component
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EventBusMetrics eventBusMetrics;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                EventBusMetrics eventBusMetrics) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventBusMetrics = eventBusMetrics;
    }

    @Override
    public void publish(DomainEvent event) {
        validate(event);
        log.info("EVENT_PUBLISHED eventType={} eventId={} aggregateType={} aggregateId={}",
                event.eventType(), event.eventId(), event.aggregateType(), event.aggregateId());
        eventBusMetrics.recordPublished(event.eventType());
        applicationEventPublisher.publishEvent(event);
    }

    private void validate(DomainEvent event) {
        if (event.eventId() == null) {
            throw new IllegalArgumentException("DomainEvent.eventId must not be null for: " + event.getClass().getSimpleName());
        }
        if (event.aggregateId() == null) {
            throw new IllegalArgumentException("DomainEvent.aggregateId must not be null for: " + event.getClass().getSimpleName());
        }
        if (event.occurredOn() == null) {
            throw new IllegalArgumentException("DomainEvent.occurredOn must not be null for: " + event.getClass().getSimpleName());
        }
    }
}
