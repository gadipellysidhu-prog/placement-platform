package com.college.placement.modules.company.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RecruiterRegisteredEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID recruiterId,
        UUID userId,
        UUID companyId,
        String companyName
) implements DomainEvent {

    public static RecruiterRegisteredEvent of(UUID recruiterId, UUID userId, UUID companyId, String companyName) {
        return new RecruiterRegisteredEvent(
                UUID.randomUUID(), Instant.now(), "Recruiter", recruiterId,
                recruiterId, userId, companyId, companyName);
    }
}
