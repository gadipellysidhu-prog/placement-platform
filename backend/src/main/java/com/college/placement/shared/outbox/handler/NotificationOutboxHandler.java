package com.college.placement.shared.outbox.handler;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.notification.domain.NotificationChannel;
import com.college.placement.modules.notification.service.NotificationService;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Routing outbox handler that turns committed domain events into delivered
 * notifications. For each supported event type it resolves the recipient from
 * the event payload, composes a message, and dispatches it via
 * {@link NotificationService} (which sends through the active email provider and
 * records delivery history).
 *
 * <p>Runs {@link Transactional} so lazy associations (e.g. {@code Student.user})
 * resolve during recipient lookup. When the recipient cannot be resolved the
 * event is logged and treated as handled — it is <strong>not</strong> retried,
 * since a missing recipient is not a transient failure. Genuine delivery
 * failures propagate so the transactional outbox retries with backoff.
 */
@Slf4j
@Component
public class NotificationOutboxHandler implements OutboxEventHandler {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    public NotificationOutboxHandler(NotificationService notificationService,
                                     AppUserRepository appUserRepository,
                                     StudentRepository studentRepository,
                                     ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void handle(OutboxEvent event) {
        log.info("OUTBOX_NOTIFICATION_HANDLE eventType={} aggregateType={} aggregateId={} outboxId={}",
                event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getId());

        Message message = composeMessage(event);
        if (message == null) {
            log.debug("NOTIFICATION_SKIPPED reason=no_template eventType={}", event.getEventType());
            return;
        }

        Optional<AppUser> recipient = resolveRecipient(event);
        if (recipient.isEmpty()) {
            log.info("NOTIFICATION_SKIPPED reason=unresolved_recipient eventType={} aggregateId={}",
                    event.getEventType(), event.getAggregateId());
            return;
        }

        notificationService.send(recipient.get(), NotificationChannel.EMAIL, message.subject(), message.body());
    }

    // ── Message composition ────────────────────────────────────────────────

    private Message composeMessage(OutboxEvent event) {
        return switch (event.getEventType()) {
            case "UserRegisteredEvent", "StudentCreatedEvent" -> new Message(
                    "Welcome to the Placement Platform",
                    "Your account has been created. Please verify your email to access placement services.");
            case "OfferCreatedEvent" -> new Message(
                    "You have received a job offer",
                    "An offer has been issued for one of your applications. Log in to review and respond.");
            case "OfferAcceptedEvent" -> new Message(
                    "Offer accepted",
                    "Your offer acceptance has been recorded. Congratulations!");
            case "OfferRejectedEvent" -> new Message(
                    "Offer declined",
                    "Your decision to decline the offer has been recorded.");
            case "ApplicationStatusChangedEvent" -> new Message(
                    "Application status updated",
                    "The status of one of your job applications has changed. Log in to view details.");
            case "CertificateVerifiedEvent" -> new Message(
                    "Certificate verified",
                    "One of your certificates has been verified and added to your profile.");
            default -> null;
        };
    }

    // ── Recipient resolution ─────────────────────────────────────────────────

    private Optional<AppUser> resolveRecipient(OutboxEvent event) {
        JsonNode payload = parsePayload(event.getPayload());

        // 1. Explicit email carried on the event.
        if (payload != null && payload.hasNonNull("email")) {
            Optional<AppUser> byEmail = appUserRepository.findByEmail(payload.get("email").asText());
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }

        // 2. AppUser-aggregate events: aggregateId is the user id.
        if ("AppUser".equals(event.getAggregateType())) {
            Optional<AppUser> byId = parseUuid(event.getAggregateId()).flatMap(appUserRepository::findById);
            if (byId.isPresent()) {
                return byId;
            }
        }

        // 3. Events carrying an explicit studentId.
        if (payload != null && payload.hasNonNull("studentId")) {
            Optional<AppUser> byStudent = parseUuid(payload.get("studentId").asText())
                    .flatMap(studentRepository::findById)
                    .map(Student::getUser);
            if (byStudent.isPresent()) {
                return byStudent;
            }
        }

        // 4. Student-aggregate events: aggregateId is the student id.
        if ("Student".equals(event.getAggregateType())) {
            return parseUuid(event.getAggregateId())
                    .flatMap(studentRepository::findById)
                    .map(Student::getUser);
        }

        return Optional.empty();
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            log.warn("NOTIFICATION_PAYLOAD_PARSE_FAILED error={}", ex.getMessage());
            return null;
        }
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private record Message(String subject, String body) {
    }
}
