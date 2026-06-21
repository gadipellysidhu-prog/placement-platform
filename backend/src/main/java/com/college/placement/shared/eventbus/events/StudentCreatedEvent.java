package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StudentCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID studentId,
        UUID userId,
        String rollNumber
) implements DomainEvent {

    public static StudentCreatedEvent of(UUID studentId, UUID userId, String rollNumber) {
        return new StudentCreatedEvent(
                UUID.randomUUID(), Instant.now(), "Student", studentId,
                studentId, userId, rollNumber);
    }
}
