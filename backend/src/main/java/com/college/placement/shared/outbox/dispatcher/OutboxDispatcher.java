package com.college.placement.shared.outbox.dispatcher;

import com.college.placement.shared.outbox.config.OutboxProperties;
import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.handler.OutboxEventHandler;
import com.college.placement.shared.outbox.metrics.OutboxMetrics;
import com.college.placement.shared.outbox.service.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class OutboxDispatcher {

    private final OutboxService outboxService;
    private final OutboxEventHandler handler;
    private final OutboxMetrics metrics;
    private final OutboxProperties properties;

    public OutboxDispatcher(OutboxService outboxService,
                            OutboxEventHandler handler,
                            OutboxMetrics metrics,
                            OutboxProperties properties) {
        this.outboxService = outboxService;
        this.handler = handler;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "${app.outbox.initial-delay:5000}",
            fixedDelayString   = "${app.outbox.fixed-delay:5000}")
    public void dispatch() {
        metrics.getDispatchTimer().record(this::dispatchCycle);
    }

    // Public so integration tests can invoke it directly without waiting for the scheduler.
    public void dispatchCycle() {
        int batchSize = properties.getBatchSize();

        List<OutboxEvent> pending = outboxService.claimPendingBatch(batchSize);
        if (!pending.isEmpty()) {
            log.debug("OUTBOX_DISPATCH claimed={} type=pending", pending.size());
            process(pending);
        }

        List<OutboxEvent> retryable = outboxService.claimRetryableBatch(batchSize, Instant.now());
        if (!retryable.isEmpty()) {
            log.debug("OUTBOX_DISPATCH claimed={} type=retryable", retryable.size());
            process(retryable);
        }
    }

    private void process(List<OutboxEvent> events) {
        for (OutboxEvent event : events) {
            try {
                handler.handle(event);
                outboxService.markProcessed(event.getId());
                metrics.recordProcessed();
            } catch (Exception ex) {
                log.warn("OUTBOX_DISPATCH_FAILED outboxId={} eventType={} error={}",
                        event.getId(), event.getEventType(), ex.getMessage());
                outboxService.markFailed(event.getId(), ex.getMessage());
                metrics.recordFailed();
            }
        }
    }
}
