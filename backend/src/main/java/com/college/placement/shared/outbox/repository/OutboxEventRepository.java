package com.college.placement.shared.outbox.repository;

import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.domain.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // ── Existing queries ──────────────────────────────────────────────────────

    List<OutboxEvent> findByStatusAndNextRetryAtBefore(OutboxStatus status, Instant now);

    Page<OutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);

    // ── Batch claim support ───────────────────────────────────────────────────

    @Query("SELECT e.id FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<UUID> findIdsByStatus(@Param("status") OutboxStatus status, Pageable pageable);

    @Query("""
            SELECT e.id FROM OutboxEvent e
            WHERE e.status = :status AND e.nextRetryAt <= :now
            ORDER BY e.nextRetryAt ASC
            """)
    List<UUID> findIdsByStatusAndNextRetryAtBefore(
            @Param("status") OutboxStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OutboxEvent e SET e.status = :target WHERE e.id IN :ids AND e.status = :current")
    int claimForProcessing(
            @Param("ids") Collection<UUID> ids,
            @Param("current") OutboxStatus current,
            @Param("target") OutboxStatus target);

    List<OutboxEvent> findByIdInAndStatus(Collection<UUID> ids, OutboxStatus status);

    // ── Metrics ───────────────────────────────────────────────────────────────

    long countByStatus(OutboxStatus status);

    // ── Dead-letter visibility ────────────────────────────────────────────────

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = com.college.placement.shared.outbox.domain.OutboxStatus.DEAD ORDER BY e.updatedAt DESC")
    Page<OutboxEvent> findDeadLetterEvents(Pageable pageable);
}
