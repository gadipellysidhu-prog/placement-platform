package com.college.placement.modules.placement.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OfferRejectedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID offerId,
        UUID applicationId,
        UUID studentId,
        UUID companyId
) implements DomainEvent {

    public static OfferRejectedEvent of(UUID offerId, UUID applicationId,
                                        UUID studentId, UUID companyId) {
        return new OfferRejectedEvent(
                UUID.randomUUID(), Instant.now(), "Offer", offerId,
                offerId, applicationId, studentId, companyId);
    }
}
