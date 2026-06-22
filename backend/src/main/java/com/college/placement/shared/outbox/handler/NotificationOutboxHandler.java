package com.college.placement.shared.outbox.handler;

import com.college.placement.shared.outbox.domain.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder notification handler for the transactional outbox.
 * Logs notification actions. Replace body with real delivery (MailHog/SMTP)
 * once the notification provider is wired up.
 */
@Slf4j
@Component
public class NotificationOutboxHandler implements OutboxEventHandler {

    @Override
    public void handle(OutboxEvent event) {
        log.info("OUTBOX_NOTIFICATION_HANDLE eventType={} aggregateType={} aggregateId={} outboxId={}",
                event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getId());

        switch (event.getEventType()) {
            case "StudentCreatedEvent" ->
                    log.info("NOTIFICATION_ACTION action=welcome_email aggregateId={}", event.getAggregateId());
            case "OfferCreatedEvent" ->
                    log.info("NOTIFICATION_ACTION action=offer_notification aggregateId={}", event.getAggregateId());
            case "OfferAcceptedEvent" ->
                    log.info("NOTIFICATION_ACTION action=offer_accepted_confirmation aggregateId={}", event.getAggregateId());
            case "ApplicationStatusChangedEvent" ->
                    log.info("NOTIFICATION_ACTION action=application_status_update aggregateId={}", event.getAggregateId());
            case "CertificateVerifiedEvent" ->
                    log.info("NOTIFICATION_ACTION action=certificate_verified aggregateId={}", event.getAggregateId());
            default ->
                    log.debug("NOTIFICATION_ACTION action=generic eventType={}", event.getEventType());
        }
    }
}
