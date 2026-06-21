package com.college.placement.modules.certificate.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CertificateRejectedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID certificateId,
        UUID studentId,
        String certificateName
) implements DomainEvent {

    public static CertificateRejectedEvent of(UUID certificateId, UUID studentId, String certificateName) {
        return new CertificateRejectedEvent(
                UUID.randomUUID(), Instant.now(), "Certificate", certificateId,
                certificateId, studentId, certificateName);
    }
}
