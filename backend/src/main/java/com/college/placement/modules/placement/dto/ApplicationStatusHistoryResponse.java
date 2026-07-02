package com.college.placement.modules.placement.dto;

import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.domain.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
        UUID id,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        String changedBy,
        Instant occurredAt
) {
    public static ApplicationStatusHistoryResponse from(ApplicationStatusHistory h) {
        return new ApplicationStatusHistoryResponse(
                h.getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedBy(),
                h.getCreatedAt()
        );
    }
}
