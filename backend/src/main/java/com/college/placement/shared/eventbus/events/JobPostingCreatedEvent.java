package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record JobPostingCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID jobPostingId,
        UUID companyId,
        String title
) implements DomainEvent {

    public static JobPostingCreatedEvent of(UUID jobPostingId, UUID companyId, String title) {
        return new JobPostingCreatedEvent(
                UUID.randomUUID(), Instant.now(), "JobPosting", jobPostingId,
                jobPostingId, companyId, title);
    }
}
