package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a student submits a certificate for verification. */
public record CertificateCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID certificateId,
        UUID studentId,
        String certificateName
) implements DomainEvent {

    public static CertificateCreatedEvent of(UUID certificateId, UUID studentId, String certificateName) {
        return new CertificateCreatedEvent(
                UUID.randomUUID(), Instant.now(), "Certificate", certificateId,
                certificateId, studentId, certificateName);
    }
}
