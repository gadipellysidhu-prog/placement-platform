package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserLockedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        String email
) implements DomainEvent {

    public static UserLockedEvent of(UUID userId, String email) {
        return new UserLockedEvent(UUID.randomUUID(), Instant.now(), "AppUser", userId, email);
    }
}
