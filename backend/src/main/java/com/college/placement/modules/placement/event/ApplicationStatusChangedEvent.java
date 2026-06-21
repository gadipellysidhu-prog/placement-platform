package com.college.placement.modules.placement.event;

import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID applicationId,
        UUID studentId,
        UUID jobPostingId,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus
) implements DomainEvent {

    public static ApplicationStatusChangedEvent of(UUID applicationId, UUID studentId,
                                                   UUID jobPostingId,
                                                   ApplicationStatus previous,
                                                   ApplicationStatus next) {
        return new ApplicationStatusChangedEvent(
                UUID.randomUUID(), Instant.now(), "JobApplication", applicationId,
                applicationId, studentId, jobPostingId, previous, next);
    }
}
