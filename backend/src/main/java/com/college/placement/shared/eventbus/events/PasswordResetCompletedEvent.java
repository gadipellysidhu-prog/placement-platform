package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetCompletedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        String email
) implements DomainEvent {

    public static PasswordResetCompletedEvent of(UUID userId, String email) {
        return new PasswordResetCompletedEvent(
                UUID.randomUUID(), Instant.now(), "AppUser", userId, email);
    }
}
