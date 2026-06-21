package com.college.placement.shared.eventbus.handler;

import com.college.placement.shared.eventbus.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emits a structured {@code EVENT_HANDLED} log line for every committed domain event.
 * Provides a low-cost observability signal without any I/O side-effects.
 */
@Slf4j
@Component
public class LoggingEventHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainEvent(DomainEvent event) {
        log.info("EVENT_HANDLED eventType={} aggregateType={} aggregateId={} occurredAt={} handler=LoggingEventHandler",
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.occurredAt());
    }
}
