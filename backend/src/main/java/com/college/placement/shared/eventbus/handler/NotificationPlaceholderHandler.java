package com.college.placement.shared.eventbus.handler;

import com.college.placement.modules.placement.event.ApplicationStatusChangedEvent;
import com.college.placement.modules.placement.event.OfferAcceptedEvent;
import com.college.placement.modules.placement.event.OfferCreatedEvent;
import com.college.placement.shared.eventbus.events.CertificateVerifiedEvent;
import com.college.placement.shared.eventbus.events.StudentCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Extension points for future notification dispatch.
 * Each method is the hook point where Phase 8 (Notification Provider) will
 * inject real email / SMS / push delivery — for now, log placeholders only.
 */
@Slf4j
@Component
public class NotificationPlaceholderHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStudentCreated(StudentCreatedEvent event) {
        log.info("EVENT_HANDLED eventType=StudentCreatedEvent handler=NotificationPlaceholderHandler" +
                " [PLACEHOLDER] Welcome notification queued for studentId={}", event.studentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOfferCreated(OfferCreatedEvent event) {
        log.info("EVENT_HANDLED eventType=OfferCreatedEvent handler=NotificationPlaceholderHandler" +
                " [PLACEHOLDER] Offer notification queued for studentId={} offerId={}",
                event.studentId(), event.offerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOfferAccepted(OfferAcceptedEvent event) {
        log.info("EVENT_HANDLED eventType=OfferAcceptedEvent handler=NotificationPlaceholderHandler" +
                " [PLACEHOLDER] Offer-accepted notification queued for studentId={} companyId={}",
                event.studentId(), event.companyId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("EVENT_HANDLED eventType=ApplicationStatusChangedEvent handler=NotificationPlaceholderHandler" +
                " [PLACEHOLDER] Status notification queued for studentId={} status={}->{}",
                event.studentId(), event.previousStatus(), event.newStatus());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCertificateVerified(CertificateVerifiedEvent event) {
        log.info("EVENT_HANDLED eventType=CertificateVerifiedEvent handler=NotificationPlaceholderHandler" +
                " [PLACEHOLDER] Certificate-verified notification queued for studentId={} certificateId={}",
                event.studentId(), event.certificateId());
    }
}
