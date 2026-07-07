package com.college.placement.shared.audit.dto;

import com.college.placement.shared.audit.domain.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Audit log entry (administrative read-only view)")
public record AuditLogResponse(
        UUID id,
        String entityType,
        String entityId,
        String action,
        String performedBy,
        String correlationId,
        String ipAddress,
        String userAgent,
        String previousValue,
        String newValue,
        String reason,
        boolean success,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getEntityType(),
                a.getEntityId(),
                a.getAction(),
                a.getPerformedBy(),
                a.getCorrelationId(),
                a.getIpAddress(),
                a.getUserAgent(),
                a.getPreviousValue(),
                a.getNewValue(),
                a.getReason(),
                a.isSuccess(),
                a.getCreatedAt()
        );
    }
}
