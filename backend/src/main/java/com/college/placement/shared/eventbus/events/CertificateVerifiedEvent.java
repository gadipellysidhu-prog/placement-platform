package com.college.placement.shared.eventbus.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CertificateVerifiedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID certificateId,
        UUID studentId,
        String certificateName
) implements DomainEvent {

    public static CertificateVerifiedEvent of(UUID certificateId, UUID studentId, String certificateName) {
        return new CertificateVerifiedEvent(
                UUID.randomUUID(), Instant.now(), "Certificate", certificateId,
                certificateId, studentId, certificateName);
    }
}
