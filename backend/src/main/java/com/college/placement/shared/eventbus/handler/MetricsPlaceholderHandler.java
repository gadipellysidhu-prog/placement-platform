package com.college.placement.shared.eventbus.handler;

import com.college.placement.modules.company.event.JobPostingOpenedEvent;
import com.college.placement.modules.placement.event.OfferAcceptedEvent;
import com.college.placement.shared.eventbus.DomainEvent;
import com.college.placement.shared.eventbus.events.ApplicationSubmittedEvent;
import com.college.placement.shared.eventbus.events.StudentCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Increments Micrometer counters for key business events.
 * Counter name: {@code domain.events.published} with tag {@code eventType}.
 * In Phase 9 (Observability) these counters will be scraped by Prometheus.
 */
@Slf4j
@Component
public class MetricsPlaceholderHandler {

    private static final String COUNTER_NAME = "domain.events.published";
    private static final String TAG_EVENT_TYPE = "eventType";

    private final MeterRegistry meterRegistry;

    public MetricsPlaceholderHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAnyDomainEvent(DomainEvent event) {
        try {
            meterRegistry.counter(COUNTER_NAME, TAG_EVENT_TYPE, event.eventType()).increment();
            log.debug("EVENT_HANDLED eventType={} handler=MetricsPlaceholderHandler counter={}",
                    event.eventType(), COUNTER_NAME);
        } catch (Exception ex) {
            log.error("EVENT_FAILED eventType={} eventId={} handler=MetricsPlaceholderHandler exception={}",
                    event.eventType(), event.eventId(), ex.getMessage(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStudentCreated(StudentCreatedEvent event) {
        meterRegistry.counter("students.registered").increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onJobPostingOpened(JobPostingOpenedEvent event) {
        meterRegistry.counter("job_postings.opened", "companyId", event.companyId().toString()).increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        meterRegistry.counter("applications.submitted", "companyId", event.companyId().toString()).increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOfferAccepted(OfferAcceptedEvent event) {
        meterRegistry.counter("offers.accepted", "companyId", event.companyId().toString()).increment();
    }
}
