package com.college.placement.modules.placement.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OfferAcceptedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID offerId,
        UUID applicationId,
        UUID studentId,
        UUID companyId
) implements DomainEvent {

    public static OfferAcceptedEvent of(UUID offerId, UUID applicationId,
                                        UUID studentId, UUID companyId) {
        return new OfferAcceptedEvent(
                UUID.randomUUID(), Instant.now(), "Offer", offerId,
                offerId, applicationId, studentId, companyId);
    }
}
