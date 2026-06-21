package com.college.placement.modules.placement.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID offerId,
        UUID applicationId,
        UUID studentId,
        UUID companyId,
        BigDecimal ctc
) implements DomainEvent {

    public static OfferCreatedEvent of(UUID offerId, UUID applicationId,
                                       UUID studentId, UUID companyId, BigDecimal ctc) {
        return new OfferCreatedEvent(
                UUID.randomUUID(), Instant.now(), "Offer", offerId,
                offerId, applicationId, studentId, companyId, ctc);
    }
}
