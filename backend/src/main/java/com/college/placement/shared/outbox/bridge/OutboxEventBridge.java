package com.college.placement.shared.outbox.bridge;

import com.college.placement.shared.eventbus.DomainEvent;
import com.college.placement.shared.outbox.service.OutboxService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the internal event bus to the transactional outbox.
 *
 * <p>Fires after the publishing transaction commits (AFTER_COMMIT) so the outbox
 * record is only written once the business state is durable. A REQUIRES_NEW
 * transaction is opened for the outbox write, ensuring the record persists
 * independently of the original transaction.
 *
 * <p>Business services must not write to the outbox directly — this handler
 * captures every {@link DomainEvent} automatically.
 */
@Slf4j
@Component
public class OutboxEventBridge {

    private final OutboxService outboxService;

    public OutboxEventBridge(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @PostConstruct
    void init() {
        log.info("OUTBOX_EVENT_BRIDGE_INITIALIZED — capturing all DomainEvent publications");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDomainEvent(DomainEvent event) {
        log.info("OUTBOX_BRIDGE_RECEIVED eventType={} aggregateType={} aggregateId={}",
                event.eventType(), event.aggregateType(), event.aggregateId());
        outboxService.saveEvent(event);
    }
}
