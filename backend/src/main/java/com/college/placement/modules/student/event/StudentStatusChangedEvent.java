package com.college.placement.modules.student.event;

import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StudentStatusChangedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID studentId,
        StudentStatus previousStatus,
        StudentStatus newStatus
) implements DomainEvent {

    public static StudentStatusChangedEvent of(UUID studentId, StudentStatus previous, StudentStatus next) {
        return new StudentStatusChangedEvent(
                UUID.randomUUID(), Instant.now(), "Student", studentId,
                studentId, previous, next);
    }
}
