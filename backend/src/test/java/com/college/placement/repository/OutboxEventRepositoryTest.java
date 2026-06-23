package com.college.placement.repository;

import com.college.placement.shared.audit.JpaAuditingConfig;
import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.domain.OutboxStatus;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class OutboxEventRepositoryTest {

    @Autowired OutboxEventRepository outboxRepository;
    @Autowired TestEntityManager em;

    // ── findByStatusAndNextRetryAtBefore ──────────────────────────────────────

    @Test
    void findByStatusAndNextRetryAtBefore_returnsMatchingEvents() {
        OutboxEvent due   = buildEvent("PENDING", Instant.now().minusSeconds(10));
        OutboxEvent future = buildEvent("PENDING", Instant.now().plusSeconds(300));
        em.persistAndFlush(due);
        em.persistAndFlush(future);

        List<OutboxEvent> results = outboxRepository
                .findByStatusAndNextRetryAtBefore(OutboxStatus.PENDING, Instant.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNextRetryAt()).isBefore(Instant.now());
    }

    // ── findIdsByStatus ───────────────────────────────────────────────────────

    @Test
    void findIdsByStatus_returnsPaginatedIds() {
        em.persistAndFlush(buildPendingEvent());
        em.persistAndFlush(buildPendingEvent());
        em.persistAndFlush(buildPendingEvent());

        List<UUID> ids = outboxRepository.findIdsByStatus(OutboxStatus.PENDING, PageRequest.of(0, 2));

        assertThat(ids).hasSize(2);
    }

    // ── findIdsByStatusAndNextRetryAtBefore ───────────────────────────────────

    @Test
    void findIdsByStatusAndNextRetryAtBefore_returnsOnlyDueIds() {
        OutboxEvent due   = buildEvent("PENDING", Instant.now().minusSeconds(5));
        OutboxEvent future = buildEvent("PENDING", Instant.now().plusSeconds(300));
        em.persistAndFlush(due);
        em.persistAndFlush(future);

        List<UUID> ids = outboxRepository.findIdsByStatusAndNextRetryAtBefore(
                OutboxStatus.PENDING, Instant.now(), PageRequest.of(0, 10));

        assertThat(ids).hasSize(1);
    }

    // ── claimForProcessing ────────────────────────────────────────────────────

    @Test
    void claimForProcessing_updatesStatusAtomically() {
        OutboxEvent e1 = em.persistAndFlush(buildPendingEvent());
        OutboxEvent e2 = em.persistAndFlush(buildPendingEvent());
        em.clear();

        int updated = outboxRepository.claimForProcessing(
                List.of(e1.getId(), e2.getId()),
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING);

        assertThat(updated).isEqualTo(2);
        assertThat(outboxRepository.countByStatus(OutboxStatus.PROCESSING)).isEqualTo(2);
        assertThat(outboxRepository.countByStatus(OutboxStatus.PENDING)).isZero();
    }

    @Test
    void claimForProcessing_doesNotUpdateAlreadyClaimedEvents() {
        OutboxEvent e = em.persistAndFlush(buildPendingEvent());
        em.clear();

        // First claim succeeds
        outboxRepository.claimForProcessing(List.of(e.getId()), OutboxStatus.PENDING, OutboxStatus.PROCESSING);

        // Second claim on same event with wrong source status → no rows updated
        int updated = outboxRepository.claimForProcessing(
                List.of(e.getId()), OutboxStatus.PENDING, OutboxStatus.PROCESSING);

        assertThat(updated).isZero();
    }

    // ── countByStatus ─────────────────────────────────────────────────────────

    @Test
    void countByStatus_returnsCorrectCount() {
        em.persistAndFlush(buildPendingEvent());
        em.persistAndFlush(buildPendingEvent());
        OutboxEvent dead = buildPendingEvent();
        dead.setStatus(OutboxStatus.DEAD);
        em.persistAndFlush(dead);

        assertThat(outboxRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(2);
        assertThat(outboxRepository.countByStatus(OutboxStatus.DEAD)).isEqualTo(1);
    }

    // ── findDeadLetterEvents ──────────────────────────────────────────────────

    @Test
    void findDeadLetterEvents_returnsOnlyDeadStatusEvents() {
        OutboxEvent dead = buildPendingEvent();
        dead.setStatus(OutboxStatus.DEAD);
        em.persistAndFlush(dead);
        em.persistAndFlush(buildPendingEvent()); // should NOT be included

        Page<OutboxEvent> result = outboxRepository.findDeadLetterEvents(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OutboxStatus.DEAD);
    }

    // ── findByIdInAndStatus ───────────────────────────────────────────────────

    @Test
    void findByIdInAndStatus_filtersCorrectly() {
        OutboxEvent e1 = em.persistAndFlush(buildPendingEvent());
        OutboxEvent e2 = em.persistAndFlush(buildPendingEvent());
        OutboxEvent processing = buildPendingEvent();
        processing.setStatus(OutboxStatus.PROCESSING);
        em.persistAndFlush(processing);

        List<OutboxEvent> results = outboxRepository.findByIdInAndStatus(
                List.of(e1.getId(), e2.getId(), processing.getId()),
                OutboxStatus.PENDING);

        assertThat(results).hasSize(2);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static OutboxEvent buildPendingEvent() {
        return buildEvent("PENDING", Instant.now().minusSeconds(1));
    }

    private static OutboxEvent buildEvent(String statusStr, Instant nextRetryAt) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("User");
        e.setAggregateId(UUID.randomUUID().toString());
        e.setEventType("UserRegistered");
        e.setPayload("{\"id\":\"" + UUID.randomUUID() + "\"}");
        e.setStatus(OutboxStatus.valueOf(statusStr));
        e.setNextRetryAt(nextRetryAt);
        return e;
    }
}
