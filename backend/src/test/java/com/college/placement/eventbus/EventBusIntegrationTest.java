package com.college.placement.eventbus;

import com.college.placement.shared.audit.domain.AuditLog;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.SpringEventPublisher;
import com.college.placement.shared.eventbus.config.EventBusConfig;
import com.college.placement.shared.eventbus.events.CompanyCreatedEvent;
import com.college.placement.shared.eventbus.events.StudentCreatedEvent;
import com.college.placement.shared.eventbus.handler.DomainEventAuditHandler;
import com.college.placement.shared.eventbus.handler.LoggingEventHandler;
import com.college.placement.shared.eventbus.handler.MetricsPlaceholderHandler;
import com.college.placement.shared.eventbus.handler.NotificationPlaceholderHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for the internal event bus.
 *
 * <p>Uses the {@code test} profile (H2 in-memory, Flyway disabled, Hibernate create-drop).
 * Test methods are NOT annotated with {@code @Transactional} so that
 * {@code @TransactionalEventListener(AFTER_COMMIT)} callbacks fire and
 * {@code DomainEventAuditHandler} can persist audit entries in its own transaction.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@ActiveProfiles("test")
class EventBusIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DomainEventAuditHandler domainEventAuditHandler;

    @Autowired
    private LoggingEventHandler loggingEventHandler;

    @Autowired
    private MetricsPlaceholderHandler metricsPlaceholderHandler;

    @Autowired
    private NotificationPlaceholderHandler notificationPlaceholderHandler;

    @Autowired
    private EventBusConfig eventBusConfig;

    // ── Infrastructure wire-up ────────────────────────────────────────────────

    @Test
    void eventPublisher_isSpringEventPublisher() {
        assertThat(eventPublisher).isInstanceOf(SpringEventPublisher.class);
    }

    @Test
    void allHandlers_areRegisteredInContext() {
        assertThat(domainEventAuditHandler).isNotNull();
        assertThat(loggingEventHandler).isNotNull();
        assertThat(metricsPlaceholderHandler).isNotNull();
        assertThat(notificationPlaceholderHandler).isNotNull();
    }

    @Test
    void eventBusConfig_isPresent() {
        assertThat(eventBusConfig).isNotNull();
    }

    // ── Event publishing ─────────────────────────────────────────────────────

    @Test
    void publishStudentCreatedEvent_doesNotThrow() {
        StudentCreatedEvent event = StudentCreatedEvent.of(UUID.randomUUID(), UUID.randomUUID(), "TEST-001");
        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publishCompanyCreatedEvent_doesNotThrow() {
        CompanyCreatedEvent event = CompanyCreatedEvent.of(UUID.randomUUID(), "Test Corp", "Tech");
        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();
    }

    // ── Audit handler — persistence ──────────────────────────────────────────

    @Test
    void publishStudentCreatedEvent_persistsAuditLog() {
        UUID studentId = UUID.randomUUID();
        StudentCreatedEvent event = StudentCreatedEvent.of(studentId, UUID.randomUUID(), "AUDIT-TEST");

        eventPublisher.publish(event);

        // fallbackExecution=true fires the handler immediately (no surrounding TX).
        // The handler opens REQUIRES_NEW, saves AuditLog, and commits.
        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("Student", studentId.toString(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("StudentCreatedEvent");
        assertThat(logs.get(0).getPerformedBy()).isEqualTo("system");
    }

    @Test
    void publishCompanyCreatedEvent_persistsAuditLog() {
        UUID companyId = UUID.randomUUID();
        CompanyCreatedEvent event = CompanyCreatedEvent.of(companyId, "Audit Corp", "Finance");

        eventPublisher.publish(event);

        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("Company", companyId.toString(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("CompanyCreatedEvent");
    }

    // ── DomainEvent contract ─────────────────────────────────────────────────

    @Test
    void studentCreatedEvent_fulfillsDomainEventContract() {
        UUID studentId = UUID.randomUUID();
        StudentCreatedEvent event = StudentCreatedEvent.of(studentId, UUID.randomUUID(), "R001");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.occurredAt()).isEqualTo(event.occurredOn());
        assertThat(event.aggregateType()).isEqualTo("Student");
        assertThat(event.aggregateId()).isEqualTo(studentId);
        assertThat(event.eventType()).isEqualTo("StudentCreatedEvent");
    }

    @Test
    void companyCreatedEvent_fulfillsDomainEventContract() {
        UUID companyId = UUID.randomUUID();
        CompanyCreatedEvent event = CompanyCreatedEvent.of(companyId, "Acme", "Manufacturing");

        assertThat(event.aggregateType()).isEqualTo("Company");
        assertThat(event.aggregateId()).isEqualTo(companyId);
        assertThat(event.eventType()).isEqualTo("CompanyCreatedEvent");
    }
}
