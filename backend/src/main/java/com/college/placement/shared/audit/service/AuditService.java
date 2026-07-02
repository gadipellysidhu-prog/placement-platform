package com.college.placement.shared.audit.service;

import com.college.placement.shared.audit.AuditContext;
import com.college.placement.shared.audit.domain.AuditLog;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Central facade for writing immutable {@link AuditLog} records for privileged
 * and mutating actions. Each write runs in a fresh {@code REQUIRES_NEW}
 * transaction so the audit trail survives even when the originating business
 * transaction rolls back, and request metadata (correlation id, client IP,
 * user agent, actor) is resolved automatically from the current context.
 *
 * <p>Complements {@code DomainEventAuditHandler}, which audits committed domain
 * events; this service is for explicit, action-level auditing from services.
 */
@Slf4j
@Service
public class AuditService {

    private static final String SYSTEM_ACTOR = "system";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Record a successful action with no before/after values. */
    public void record(String entityType, String entityId, String action) {
        record(entityType, entityId, action, null, null, null, true);
    }

    /** Record an action with before/after values and a reason. */
    public void record(String entityType, String entityId, String action,
                       String previousValue, String newValue, String reason) {
        record(entityType, entityId, action, previousValue, newValue, reason, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityType, String entityId, String action,
                       String previousValue, String newValue, String reason, boolean success) {
        try {
            AuditLog entry = new AuditLog();
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setAction(action);
            entry.setPerformedBy(currentActor());
            entry.setPreviousValue(previousValue);
            entry.setNewValue(newValue);
            entry.setReason(reason);
            entry.setSuccess(success);
            entry.setCorrelationId(AuditContext.correlationId());
            entry.setIpAddress(AuditContext.ipAddress());
            entry.setUserAgent(AuditContext.userAgent());
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Auditing must never break the originating operation.
            log.error("AUDIT_WRITE_FAILED entityType={} entityId={} action={} exception={}",
                    entityType, entityId, action, ex.getMessage(), ex);
        }
    }

    /** Convenience overload for entities keyed by {@link UUID}. */
    public void record(String entityType, UUID entityId, String action) {
        record(entityType, entityId == null ? "unknown" : entityId.toString(), action);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return auth.getName();
        }
        return SYSTEM_ACTOR;
    }
}
