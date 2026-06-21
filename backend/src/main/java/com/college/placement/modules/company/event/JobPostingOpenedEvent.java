package com.college.placement.modules.company.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record JobPostingOpenedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID jobPostingId,
        UUID companyId,
        String title
) implements DomainEvent {

    public static JobPostingOpenedEvent of(UUID jobPostingId, UUID companyId, String title) {
        return new JobPostingOpenedEvent(
                UUID.randomUUID(), Instant.now(), "JobPosting", jobPostingId,
                jobPostingId, companyId, title);
    }
}
