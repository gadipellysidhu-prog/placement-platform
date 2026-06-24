package com.college.placement.shared.eventbus.handler;

import com.college.placement.shared.eventbus.DomainEvent;
import com.college.placement.shared.eventbus.metrics.EventBusMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emits structured EVENT_RECEIVED / EVENT_HANDLED log lines for every committed domain event.
 * Provides low-cost observability without any I/O side-effects.
 * Handler failures are isolated: exceptions are logged and metered but never rethrown.
 */
@Slf4j
@Component
public class LoggingEventHandler {

    private static final String HANDLER_NAME = "LoggingEventHandler";

    private final EventBusMetrics eventBusMetrics;

    public LoggingEventHandler(EventBusMetrics eventBusMetrics) {
        this.eventBusMetrics = eventBusMetrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainEvent(DomainEvent event) {
        try {
            log.info("EVENT_RECEIVED eventType={} eventId={} aggregateType={} aggregateId={}",
                    event.eventType(), event.eventId(), event.aggregateType(), event.aggregateId());
            log.info("EVENT_HANDLED eventType={} aggregateType={} aggregateId={} occurredAt={} handler={}",
                    event.eventType(), event.aggregateType(), event.aggregateId(),
                    event.occurredAt(), HANDLER_NAME);
            eventBusMetrics.recordHandled(event.eventType(), HANDLER_NAME);
        } catch (Exception ex) {
            log.error("EVENT_FAILED eventType={} eventId={} handler={} exception={}",
                    event.eventType(), event.eventId(), HANDLER_NAME, ex.getMessage(), ex);
            eventBusMetrics.recordFailed(event.eventType(), HANDLER_NAME);
        }
    }
}
