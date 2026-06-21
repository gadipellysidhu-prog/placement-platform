package com.college.placement.modules.student.event;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StudentSkillRemovedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID studentId,
        UUID skillId,
        String skillName
) implements DomainEvent {

    public static StudentSkillRemovedEvent of(UUID studentId, UUID skillId, String skillName) {
        return new StudentSkillRemovedEvent(
                UUID.randomUUID(), Instant.now(), "Student", studentId,
                studentId, skillId, skillName);
    }
}
