package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ApplicationSubmittedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID applicationId,
        UUID studentId,
        UUID jobPostingId,
        UUID companyId
) implements DomainEvent {

    public static ApplicationSubmittedEvent of(UUID applicationId, UUID studentId,
                                               UUID jobPostingId, UUID companyId) {
        return new ApplicationSubmittedEvent(
                UUID.randomUUID(), Instant.now(), "JobApplication", applicationId,
                applicationId, studentId, jobPostingId, companyId);
    }
}
