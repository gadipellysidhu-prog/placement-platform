package com.college.placement.shared.outbox.service;

import com.college.placement.shared.eventbus.DomainEvent;
import com.college.placement.shared.outbox.config.OutboxProperties;
import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.domain.OutboxStatus;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    public OutboxService(OutboxEventRepository repository,
                         ObjectMapper objectMapper,
                         OutboxProperties properties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    public OutboxEvent saveEvent(DomainEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(event.aggregateType());
        outboxEvent.setAggregateId(event.aggregateId().toString());
        outboxEvent.setEventType(event.eventType());
        outboxEvent.setPayload(serialize(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setRetryCount(0);

        OutboxEvent saved = repository.save(outboxEvent);
        log.info("OUTBOX_SAVED eventType={} aggregateType={} aggregateId={} outboxId={}",
                saved.getEventType(), saved.getAggregateType(), saved.getAggregateId(), saved.getId());
        return saved;
    }

    public void markProcessed(UUID id) {
        OutboxEvent event = requireEvent(id);
        event.setStatus(OutboxStatus.SENT);
        event.setProcessedAt(Instant.now());
        event.setErrorMessage(null);
        repository.save(event);
        log.info("OUTBOX_PROCESSED outboxId={} eventType={}", id, event.getEventType());
    }

    public void markFailed(UUID id, String errorMessage) {
        OutboxEvent event = requireEvent(id);

        if (event.getRetryCount() >= properties.getMaxRetries()) {
            markDeadLetter(event, errorMessage);
        } else {
            scheduleRetry(event, errorMessage);
        }
    }

    public void scheduleRetry(OutboxEvent event, String errorMessage) {
        int[] backoff = properties.getRetryBackoffMinutes();
        int index = Math.min(event.getRetryCount(), backoff.length - 1);
        Instant nextRetry = Instant.now().plus(backoff[index], ChronoUnit.MINUTES);

        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(nextRetry);
        event.setErrorMessage(errorMessage);
        repository.save(event);

        log.warn("OUTBOX_RETRY_SCHEDULED outboxId={} retryCount={} nextRetryAt={} eventType={}",
                event.getId(), event.getRetryCount(), nextRetry, event.getEventType());
    }

    // ── Batch claim (for dispatcher) ──────────────────────────────────────────

    public List<OutboxEvent> claimPendingBatch(int batchSize) {
        List<UUID> ids = repository.findIdsByStatus(OutboxStatus.PENDING, PageRequest.of(0, batchSize));
        if (ids.isEmpty()) return List.of();
        repository.claimForProcessing(ids, OutboxStatus.PENDING, OutboxStatus.PROCESSING);
        return repository.findByIdInAndStatus(ids, OutboxStatus.PROCESSING);
    }

    public List<OutboxEvent> claimRetryableBatch(int batchSize, Instant now) {
        List<UUID> ids = repository.findIdsByStatusAndNextRetryAtBefore(
                OutboxStatus.FAILED, now, PageRequest.of(0, batchSize));
        if (ids.isEmpty()) return List.of();
        repository.claimForProcessing(ids, OutboxStatus.FAILED, OutboxStatus.PROCESSING);
        return repository.findByIdInAndStatus(ids, OutboxStatus.PROCESSING);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void markDeadLetter(OutboxEvent event, String errorMessage) {
        event.setStatus(OutboxStatus.DEAD);
        event.setErrorMessage(errorMessage);
        repository.save(event);
        log.error("OUTBOX_DEAD_LETTER outboxId={} eventType={} retryCount={} error={}",
                event.getId(), event.getEventType(), event.getRetryCount(), errorMessage);
    }

    private OutboxEvent requireEvent(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OutboxEvent not found: " + id));
    }

    private String serialize(DomainEvent event) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventClass", event.getClass().getName());
            payload.putAll(objectMapper.convertValue(event, new TypeReference<Map<String, Object>>() {}));
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.eventType(), e);
        }
    }
}
