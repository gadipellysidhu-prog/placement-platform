package com.college.placement.shared.eventbus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Delegates to Spring's ApplicationEventPublisher.
 * Emits a structured EVENT_PUBLISHED log line before dispatching so the publishing
 * side is always observable independently of handler execution.
 */
@Slf4j
@Component
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.info("EVENT_PUBLISHED eventType={} aggregateType={} aggregateId={} eventId={}",
                event.eventType(), event.aggregateType(), event.aggregateId(), event.eventId());
        applicationEventPublisher.publishEvent(event);
    }
}
