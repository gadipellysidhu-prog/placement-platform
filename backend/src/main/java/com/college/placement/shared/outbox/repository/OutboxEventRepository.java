package com.college.placement.shared.outbox.repository;

import com.college.placement.shared.outbox.domain.OutboxEvent;
import com.college.placement.shared.outbox.domain.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusAndNextRetryAtBefore(OutboxStatus status, Instant now);

    Page<OutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);
}
