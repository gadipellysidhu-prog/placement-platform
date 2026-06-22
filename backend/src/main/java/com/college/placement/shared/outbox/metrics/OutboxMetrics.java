package com.college.placement.shared.outbox.metrics;

import com.college.placement.shared.outbox.domain.OutboxStatus;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Timer dispatchTimer;

    public OutboxMetrics(OutboxEventRepository repository, MeterRegistry registry) {
        Gauge.builder("outbox.pending.count", repository,
                        r -> r.countByStatus(OutboxStatus.PENDING))
                .description("Pending outbox events awaiting dispatch")
                .register(registry);

        Gauge.builder("outbox.deadletter.count", repository,
                        r -> r.countByStatus(OutboxStatus.DEAD))
                .description("Dead-letter outbox events requiring manual recovery")
                .register(registry);

        this.processedCounter = Counter.builder("outbox.processed.count")
                .description("Total outbox events successfully processed")
                .register(registry);

        this.failedCounter = Counter.builder("outbox.failed.count")
                .description("Total outbox events that failed processing")
                .register(registry);

        this.dispatchTimer = Timer.builder("outbox.dispatch.duration")
                .description("Duration of a single outbox dispatch cycle")
                .register(registry);
    }

    public void recordProcessed() {
        processedCounter.increment();
    }

    public void recordFailed() {
        failedCounter.increment();
    }

    public Timer getDispatchTimer() {
        return dispatchTimer;
    }
}
