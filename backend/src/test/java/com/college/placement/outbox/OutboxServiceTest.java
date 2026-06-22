package com.college.placement.outbox;

import com.college.placement.shared.eventbus.DomainEvent;
import com.college.placement.shared.outbox.config.OutboxProperties;
import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.domain.OutboxStatus;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import com.college.placement.shared.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;

    private OutboxService service;

    @BeforeEach
    void setUp() {
        OutboxProperties props = new OutboxProperties();
        props.setMaxRetries(3);
        props.setRetryBackoffMinutes(new int[]{1, 5, 15});
        service = new OutboxService(repository, new ObjectMapper(), props);
    }

    // ── saveEvent ─────────────────────────────────────────────────────────────

    @Test
    void saveEvent_persistsOutboxEventWithPendingStatus() {
        DomainEvent event = sampleEvent();
        OutboxEvent stub = new OutboxEvent();
        stub.setStatus(OutboxStatus.PENDING);
        when(repository.save(any())).thenReturn(stub);

        service.saveEvent(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAggregateType()).isEqualTo("Student");
        assertThat(saved.getAggregateId()).isEqualTo(event.aggregateId().toString());
        assertThat(saved.getEventType()).isEqualTo("TestDomainEvent");
        assertThat(saved.getPayload()).contains("eventClass");
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void saveEvent_payloadContainsFullyQualifiedClassName() {
        DomainEvent event = sampleEvent();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveEvent(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("OutboxServiceTest");
    }

    // ── markProcessed ─────────────────────────────────────────────────────────

    @Test
    void markProcessed_setsStatusToSentAndProcessedAt() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = pendingEvent(id);
        when(repository.findById(id)).thenReturn(Optional.of(event));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markProcessed(id);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getErrorMessage()).isNull();
    }

    @Test
    void markProcessed_throwsWhenEventNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markProcessed(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(id.toString());
    }

    // ── markFailed / retry scheduling ─────────────────────────────────────────

    @Test
    void markFailed_belowMaxRetries_schedulesRetryWithBackoff() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = pendingEvent(id);
        event.setRetryCount(0); // first failure
        when(repository.findById(id)).thenReturn(Optional.of(event));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        service.markFailed(id, "timeout");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isAfter(before);
        assertThat(event.getErrorMessage()).isEqualTo("timeout");
    }

    @Test
    void markFailed_secondAttempt_usesSecondBackoffSlot() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = pendingEvent(id);
        event.setRetryCount(1);
        when(repository.findById(id)).thenReturn(Optional.of(event));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        service.markFailed(id, "err");

        // backoff[1] = 5 min, so nextRetryAt should be ~5 min from now
        assertThat(event.getNextRetryAt()).isAfter(before.plusSeconds(240));
        assertThat(event.getRetryCount()).isEqualTo(2);
    }

    @Test
    void markFailed_atMaxRetries_marksDeadLetter() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = pendingEvent(id);
        event.setRetryCount(3); // maxRetries=3
        when(repository.findById(id)).thenReturn(Optional.of(event));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markFailed(id, "permanent failure");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getErrorMessage()).isEqualTo("permanent failure");
    }

    // ── Retry backoff calculation ─────────────────────────────────────────────

    @Test
    void scheduleRetry_clampsBackoffIndexToArrayBounds() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        event.setRetryCount(10); // beyond array length (3 elements)
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Use scheduleRetry directly to bypass maxRetries check
        service.scheduleRetry(event, "overflow");

        // Should clamp to last element: backoff[2] = 15 min
        assertThat(event.getNextRetryAt()).isAfter(Instant.now().plusSeconds(14 * 60));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DomainEvent sampleEvent() {
        UUID id = UUID.randomUUID();
        return new TestDomainEvent(id);
    }

    private OutboxEvent pendingEvent(UUID id) {
        OutboxEvent e = new OutboxEvent();
        e.setStatus(OutboxStatus.PENDING);
        e.setEventType("TestDomainEvent");
        return e;
    }

    record TestDomainEvent(UUID aggregateId) implements DomainEvent {
        @Override public UUID eventId() { return UUID.randomUUID(); }
        @Override public Instant occurredOn() { return Instant.now(); }
        @Override public String aggregateType() { return "Student"; }
    }
}
