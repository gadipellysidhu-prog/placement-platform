package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        String email,
        String role
) implements DomainEvent {

    public static UserRegisteredEvent of(UUID userId, String email, String role) {
        return new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), "AppUser", userId,
                email, role);
    }
}
