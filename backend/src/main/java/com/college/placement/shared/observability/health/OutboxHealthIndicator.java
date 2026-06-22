package com.college.placement.shared.observability.health;

import com.college.placement.shared.outbox.domain.OutboxStatus;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("outbox")
public class OutboxHealthIndicator implements HealthIndicator {

    private static final long DEAD_LETTER_THRESHOLD = 1;
    private static final long FAILED_WARNING_THRESHOLD = 20;

    private final OutboxEventRepository outboxRepository;

    public OutboxHealthIndicator(OutboxEventRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public Health health() {
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long failed  = outboxRepository.countByStatus(OutboxStatus.FAILED);
        long dead    = outboxRepository.countByStatus(OutboxStatus.DEAD);

        Health.Builder builder = (dead >= DEAD_LETTER_THRESHOLD)
                ? Health.down().withDetail("reason", "Dead-letter events require manual intervention")
                : Health.up();

        return builder
                .withDetail("pending", pending)
                .withDetail("failed", failed)
                .withDetail("dead", dead)
                .withDetail("failedHighWatermark", failed >= FAILED_WARNING_THRESHOLD)
                .build();
    }
}
