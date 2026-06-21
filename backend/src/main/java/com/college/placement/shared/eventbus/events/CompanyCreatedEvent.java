package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CompanyCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID companyId,
        String companyName,
        String industry
) implements DomainEvent {

    public static CompanyCreatedEvent of(UUID companyId, String companyName, String industry) {
        return new CompanyCreatedEvent(
                UUID.randomUUID(), Instant.now(), "Company", companyId,
                companyId, companyName, industry);
    }
}
