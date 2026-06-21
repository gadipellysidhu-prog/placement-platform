package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StudentUpdatedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID studentId
) implements DomainEvent {

    public static StudentUpdatedEvent of(UUID studentId) {
        return new StudentUpdatedEvent(
                UUID.randomUUID(), Instant.now(), "Student", studentId, studentId);
    }
}
