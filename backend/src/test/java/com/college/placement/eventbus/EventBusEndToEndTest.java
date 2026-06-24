package com.college.placement.eventbus;

import com.college.placement.modules.certificate.event.CertificateRejectedEvent;
import com.college.placement.modules.company.event.RecruiterRegisteredEvent;
import com.college.placement.modules.placement.event.OfferCreatedEvent;
import com.college.placement.modules.student.event.StudentSkillAddedEvent;
import com.college.placement.shared.audit.domain.AuditLog;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.metrics.EventBusMetrics;
import com.college.placement.shared.eventbus.events.UserRegisteredEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * End-to-end integration tests for the internal event bus.
 * Verifies that events traverse: Publisher -> EventBus -> Listeners -> Handlers.
 *
 * <p>Tests are NOT annotated with {@code @Transactional} so that
 * {@code @TransactionalEventListener(AFTER_COMMIT)} callbacks fire
 * and the DomainEventAuditHandler can persist audit entries.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@ActiveProfiles("test")
class EventBusEndToEndTest {

    @Autowired private EventPublisher eventPublisher;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private MeterRegistry meterRegistry;

    // ── Auth: UserRegisteredEvent ────────────────────────────────────────────

    @Test
    void authFlow_userRegisteredEvent_publishesAndHandles() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.of(userId, "test@example.com", "STUDENT");

        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();

        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("AppUser", userId.toString(), Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("UserRegisteredEvent");
        assertThat(logs.get(0).getPerformedBy()).isEqualTo("system");
    }

    @Test
    void authFlow_userRegisteredEvent_fulfillsDomainEventContract() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.of(userId, "contract@test.com", "ADMIN");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(userId);
        assertThat(event.aggregateType()).isEqualTo("AppUser");
        assertThat(event.eventType()).isEqualTo("UserRegisteredEvent");
    }

    // ── Student: StudentSkillAddedEvent ──────────────────────────────────────

    @Test
    void studentFlow_studentSkillAddedEvent_publishesAndHandles() {
        UUID studentId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        StudentSkillAddedEvent event = StudentSkillAddedEvent.of(studentId, skillId, "Java");

        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();

        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("Student", studentId.toString(), Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("StudentSkillAddedEvent");
    }

    @Test
    void studentFlow_studentSkillAddedEvent_fulfillsDomainEventContract() {
        UUID studentId = UUID.randomUUID();
        StudentSkillAddedEvent event = StudentSkillAddedEvent.of(studentId, UUID.randomUUID(), "Python");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(studentId);
        assertThat(event.aggregateType()).isEqualTo("Student");
        assertThat(event.eventType()).isEqualTo("StudentSkillAddedEvent");
        assertThat(event.skillName()).isEqualTo("Python");
    }

    // ── Company: RecruiterRegisteredEvent ────────────────────────────────────

    @Test
    void companyFlow_recruiterRegisteredEvent_publishesAndHandles() {
        UUID recruiterId = UUID.randomUUID();
        RecruiterRegisteredEvent event = RecruiterRegisteredEvent.of(
                recruiterId, UUID.randomUUID(), UUID.randomUUID(), "Tech Corp");

        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();

        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("Recruiter", recruiterId.toString(), Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("RecruiterRegisteredEvent");
    }

    @Test
    void companyFlow_recruiterRegisteredEvent_fulfillsDomainEventContract() {
        UUID recruiterId = UUID.randomUUID();
        RecruiterRegisteredEvent event = RecruiterRegisteredEvent.of(
                recruiterId, UUID.randomUUID(), UUID.randomUUID(), "Acme Inc");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(recruiterId);
        assertThat(event.aggregateType()).isEqualTo("Recruiter");
        assertThat(event.eventType()).isEqualTo("RecruiterRegisteredEvent");
        assertThat(event.companyName()).isEqualTo("Acme Inc");
    }

    // ── Placement: OfferCreatedEvent ─────────────────────────────────────────

    @Test
    void placementFlow_offerCreatedEvent_publishesAndHandles() {
        UUID offerId = UUID.randomUUID();
        OfferCreatedEvent event = OfferCreatedEvent.of(
                offerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1200000"));

        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();

        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("Offer", offerId.toString(), Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("OfferCreatedEvent");
    }

    @Test
    void placementFlow_offerCreatedEvent_fulfillsDomainEventContract() {
        UUID offerId = UUID.randomUUID();
        OfferCreatedEvent event = OfferCreatedEvent.of(
                offerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("800000"));

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(offerId);
        assertThat(event.aggregateType()).isEqualTo("Offer");
        assertThat(event.eventType()).isEqualTo("OfferCreatedEvent");
        assertThat(event.ctc()).isEqualByComparingTo("800000");
    }

    // ── Certificate: CertificateRejectedEvent ────────────────────────────────

    @Test
    void certificateFlow_certificateRejectedEvent_publishesAndHandles() {
        UUID certificateId = UUID.randomUUID();
        CertificateRejectedEvent event = CertificateRejectedEvent.of(
                certificateId, UUID.randomUUID(), "AWS Solutions Architect");

        assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();

        List<AuditLog> logs = auditLogRepository
                .findByEntityTypeAndEntityId("Certificate", certificateId.toString(), Pageable.unpaged())
                .getContent();

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("CertificateRejectedEvent");
    }

    @Test
    void certificateFlow_certificateRejectedEvent_fulfillsDomainEventContract() {
        UUID certificateId = UUID.randomUUID();
        CertificateRejectedEvent event = CertificateRejectedEvent.of(
                certificateId, UUID.randomUUID(), "Google Cloud Professional");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(certificateId);
        assertThat(event.aggregateType()).isEqualTo("Certificate");
        assertThat(event.eventType()).isEqualTo("CertificateRejectedEvent");
        assertThat(event.certificateName()).isEqualTo("Google Cloud Professional");
    }

    // ── Metrics validation ───────────────────────────────────────────────────

    @Test
    void publishAnyEvent_incrementsEventbusPublishedMetric() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.of(userId, "metrics@test.com", "STUDENT");

        double before = meterRegistry.find(EventBusMetrics.METRIC_PUBLISHED)
                .tag("eventType", "UserRegisteredEvent")
                .counter() != null
                ? meterRegistry.find(EventBusMetrics.METRIC_PUBLISHED)
                        .tag("eventType", "UserRegisteredEvent").counter().count()
                : 0.0;

        eventPublisher.publish(event);

        double after = meterRegistry.find(EventBusMetrics.METRIC_PUBLISHED)
                .tag("eventType", "UserRegisteredEvent")
                .counter().count();

        assertThat(after).isGreaterThan(before);
    }

    @Test
    void publishAnyEvent_incrementsEventbusHandledMetric() {
        UUID studentId = UUID.randomUUID();
        StudentSkillAddedEvent event = StudentSkillAddedEvent.of(studentId, UUID.randomUUID(), "Spring");

        eventPublisher.publish(event);

        double count = meterRegistry.find(EventBusMetrics.METRIC_HANDLED)
                .tag("eventType", "StudentSkillAddedEvent")
                .counters().stream()
                .mapToDouble(c -> c.count())
                .sum();

        assertThat(count).isGreaterThan(0);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    void publish_nullEventId_throwsIllegalArgument() {
        // Directly test validation via a hand-crafted invalid event
        var invalidEvent = new com.college.placement.shared.eventbus.DomainEvent() {
            public java.util.UUID eventId() { return null; }
            public java.time.Instant occurredOn() { return java.time.Instant.now(); }
            public String aggregateType() { return "Test"; }
            public java.util.UUID aggregateId() { return java.util.UUID.randomUUID(); }
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> eventPublisher.publish(invalidEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");
    }

    @Test
    void publish_nullAggregateId_throwsIllegalArgument() {
        var invalidEvent = new com.college.placement.shared.eventbus.DomainEvent() {
            public java.util.UUID eventId() { return java.util.UUID.randomUUID(); }
            public java.time.Instant occurredOn() { return java.time.Instant.now(); }
            public String aggregateType() { return "Test"; }
            public java.util.UUID aggregateId() { return null; }
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> eventPublisher.publish(invalidEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aggregateId");
    }
}
