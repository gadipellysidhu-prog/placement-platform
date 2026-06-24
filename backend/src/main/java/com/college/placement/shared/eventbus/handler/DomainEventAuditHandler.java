package com.college.placement.shared.eventbus.handler;

import com.college.placement.shared.audit.domain.AuditLog;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import com.college.placement.shared.eventbus.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists an {@link AuditLog} row for every committed domain event.
 *
 * <p>Runs in a fresh {@code REQUIRES_NEW} transaction so that the audit write
 * is independent of — and cannot be rolled back by — the originating business transaction.
 */
@Slf4j
@Component
public class DomainEventAuditHandler {

    private final AuditLogRepository auditLogRepository;

    public DomainEventAuditHandler(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDomainEvent(DomainEvent event) {
        try {
            log.info("EVENT_HANDLED eventType={} aggregateId={} handler=DomainEventAuditHandler",
                    event.eventType(), event.aggregateId());

            AuditLog entry = new AuditLog();
            entry.setEntityType(event.aggregateType());
            entry.setEntityId(event.aggregateId().toString());
            entry.setAction(event.eventType());
            entry.setPerformedBy("system");

            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("EVENT_FAILED eventType={} eventId={} handler=DomainEventAuditHandler exception={}",
                    event.eventType(), event.eventId(), ex.getMessage(), ex);
        }
    }
}
