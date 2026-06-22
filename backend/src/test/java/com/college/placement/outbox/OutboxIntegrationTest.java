package com.college.placement.outbox;

import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.StudentCreatedEvent;
import com.college.placement.shared.eventbus.events.UserRegisteredEvent;
import com.college.placement.shared.outbox.dispatcher.OutboxDispatcher;
import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.domain.OutboxStatus;
import com.college.placement.shared.outbox.handler.NotificationOutboxHandler;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import com.college.placement.shared.outbox.service.OutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Integration tests for the Transactional Outbox Pattern.
 *
 * <p>Tests run against H2 (test profile). The scheduler's initial-delay is set to 1 hour
 * so it will not fire automatically; each test drives the dispatcher manually.
 *
 * <p>Test methods are NOT annotated with {@code @Transactional} so that
 * {@code @TransactionalEventListener(AFTER_COMMIT)} callbacks fire correctly
 * and outbox records are created in their own REQUIRES_NEW transactions.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@ActiveProfiles("test")
class OutboxIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxDispatcher dispatcher;

    @SpyBean
    private NotificationOutboxHandler notificationHandler;

    @AfterEach
    void cleanup() {
        reset(notificationHandler);
        outboxRepository.deleteAll();
    }

    // ── Bridge: DomainEvent → OutboxEvent ─────────────────────────────────────

    @Test
    void publishDomainEvent_createsOutboxRecord() {
        UUID studentId = UUID.randomUUID();
        StudentCreatedEvent event = StudentCreatedEvent.of(studentId, UUID.randomUUID(), "OUTBOX-TEST-001");

        eventPublisher.publish(event);

        List<OutboxEvent> records = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(studentId.toString()))
                .toList();

        assertThat(records).hasSize(1);
        OutboxEvent outbox = records.get(0);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getEventType()).isEqualTo("StudentCreatedEvent");
        assertThat(outbox.getAggregateType()).isEqualTo("Student");
        assertThat(outbox.getPayload()).contains("eventClass");
        assertThat(outbox.getRetryCount()).isZero();
    }

    @Test
    void publishDomainEvent_payloadContainsAggregateId() {
        UUID studentId = UUID.randomUUID();
        StudentCreatedEvent event = StudentCreatedEvent.of(studentId, UUID.randomUUID(), "PAY-TEST");

        eventPublisher.publish(event);

        OutboxEvent outbox = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(studentId.toString()))
                .findFirst()
                .orElseThrow();

        assertThat(outbox.getPayload()).contains(studentId.toString());
    }

    // ── Dispatcher: happy path ────────────────────────────────────────────────

    @Test
    void dispatcher_processesPendingEvent_marksAsSent() {
        UUID studentId = UUID.randomUUID();
        eventPublisher.publish(StudentCreatedEvent.of(studentId, UUID.randomUUID(), "DISPATCH-TEST"));

        // Ensure the bridge saved the outbox record before dispatching
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        assertThat(pending).isPositive();

        dispatcher.dispatchCycle();

        OutboxEvent processed = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(studentId.toString()))
                .findFirst()
                .orElseThrow();

        assertThat(processed.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(processed.getProcessedAt()).isNotNull();
    }

    // ── Dispatcher: retry flow ────────────────────────────────────────────────

    @Test
    void dispatcher_onHandlerFailure_setsFailedAndIncrementsRetryCount() {
        doThrow(new RuntimeException("simulated failure")).when(notificationHandler).handle(any());

        UUID studentId = UUID.randomUUID();
        eventPublisher.publish(StudentCreatedEvent.of(studentId, UUID.randomUUID(), "RETRY-TEST"));

        dispatcher.dispatchCycle();

        OutboxEvent failed = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(studentId.toString()))
                .findFirst()
                .orElseThrow();

        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getNextRetryAt()).isAfter(Instant.now());
        assertThat(failed.getErrorMessage()).contains("simulated failure");
    }

    @Test
    void dispatcher_retryableEvent_isProcessedOnNextCycle() {
        doThrow(new RuntimeException("first fail")).when(notificationHandler).handle(any());

        UUID studentId = UUID.randomUUID();
        eventPublisher.publish(StudentCreatedEvent.of(studentId, UUID.randomUUID(), "RETRY-CYCLE"));

        // First dispatch — fails
        dispatcher.dispatchCycle();

        OutboxEvent failed = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(studentId.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);

        // Manually push nextRetryAt into the past so it's eligible for retry
        failed.setNextRetryAt(Instant.now().minusSeconds(10));
        outboxRepository.save(failed);

        // Reset handler to succeed on second attempt
        reset(notificationHandler);

        dispatcher.dispatchCycle();

        OutboxEvent retried = outboxRepository.findById(failed.getId()).orElseThrow();
        assertThat(retried.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    // ── Dead-letter flow ──────────────────────────────────────────────────────

    @Test
    void dispatcher_afterMaxRetries_marksAsDeadLetter() {
        doThrow(new RuntimeException("permanent error")).when(notificationHandler).handle(any());

        UUID studentId = UUID.randomUUID();
        eventPublisher.publish(StudentCreatedEvent.of(studentId, UUID.randomUUID(), "DEAD-LETTER-TEST"));

        // maxRetries=3 in test profile → need 4 total dispatch cycles (1 initial + 3 retries)
        for (int i = 0; i < 4; i++) {
            List<OutboxEvent> all = outboxRepository.findAll().stream()
                    .filter(e -> e.getAggregateId().equals(studentId.toString()))
                    .toList();
            // Push nextRetryAt into the past so retryable events are eligible
            for (OutboxEvent e : all) {
                if (e.getStatus() == OutboxStatus.FAILED && e.getNextRetryAt() != null) {
                    e.setNextRetryAt(Instant.now().minusSeconds(10));
                    outboxRepository.save(e);
                }
            }
            dispatcher.dispatchCycle();
        }

        OutboxEvent dead = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(studentId.toString()))
                .findFirst()
                .orElseThrow();

        assertThat(dead.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(dead.getErrorMessage()).isNotBlank();
    }

    // ── UserRegisteredEvent → outbox (verifies the auth registration flow) ────

    @Test
    void publishUserRegisteredEvent_createsOutboxRecord() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.of(userId, "test@example.com", "ROLE_STUDENT");

        eventPublisher.publish(event);

        OutboxEvent outbox = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(userId.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No outbox row found for UserRegisteredEvent"));

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getEventType()).isEqualTo("UserRegisteredEvent");
        assertThat(outbox.getAggregateType()).isEqualTo("AppUser");
        assertThat(outbox.getPayload()).contains("test@example.com");
    }

    @Test
    void publishUserRegisteredEvent_dispatcherProcessesIt() {
        UUID userId = UUID.randomUUID();
        eventPublisher.publish(UserRegisteredEvent.of(userId, "dispatch@example.com", "ROLE_STUDENT"));

        dispatcher.dispatchCycle();

        OutboxEvent outbox = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(userId.toString()))
                .findFirst()
                .orElseThrow();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(outbox.getProcessedAt()).isNotNull();
    }

    // ── Repository queries ────────────────────────────────────────────────────

    @Test
    void countByStatus_returnsCorrectCount() {
        eventPublisher.publish(StudentCreatedEvent.of(UUID.randomUUID(), UUID.randomUUID(), "COUNT-A"));
        eventPublisher.publish(StudentCreatedEvent.of(UUID.randomUUID(), UUID.randomUUID(), "COUNT-B"));

        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        assertThat(pending).isGreaterThanOrEqualTo(2);
    }

    @Test
    void findDeadLetterEvents_returnsDeadEvents() {
        UUID studentId = UUID.randomUUID();
        doThrow(new RuntimeException("dead")).when(notificationHandler).handle(any());

        eventPublisher.publish(StudentCreatedEvent.of(studentId, UUID.randomUUID(), "DEAD-QUERY"));

        // Drive to DEAD state
        for (int i = 0; i < 4; i++) {
            outboxRepository.findAll().stream()
                    .filter(e -> e.getStatus() == OutboxStatus.FAILED)
                    .forEach(e -> {
                        e.setNextRetryAt(Instant.now().minusSeconds(5));
                        outboxRepository.save(e);
                    });
            dispatcher.dispatchCycle();
        }

        long deadCount = outboxRepository.countByStatus(OutboxStatus.DEAD);
        assertThat(deadCount).isPositive();
    }
}
