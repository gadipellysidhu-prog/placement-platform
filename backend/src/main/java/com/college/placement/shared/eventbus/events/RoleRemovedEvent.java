package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RoleRemovedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        String email,
        String role
) implements DomainEvent {

    public static RoleRemovedEvent of(UUID userId, String email, String role) {
        return new RoleRemovedEvent(UUID.randomUUID(), Instant.now(), "AppUser", userId, email, role);
    }
}
