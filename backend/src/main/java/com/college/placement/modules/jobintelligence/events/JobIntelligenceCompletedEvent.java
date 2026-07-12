package com.college.placement.modules.jobintelligence.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an AI extraction run reaches a terminal state. The outbox picks
 * this up and (via NotificationOutboxHandler) emails the requesting officer.
 */
public record JobIntelligenceCompletedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID runId,
        UUID jobPostingId,
        String postingTitle,
        boolean success,
        int skillsTagged,
        String requestedBy
) implements DomainEvent {

    public static JobIntelligenceCompletedEvent of(UUID runId, UUID jobPostingId, String postingTitle,
                                                   boolean success, int skillsTagged, String requestedBy) {
        return new JobIntelligenceCompletedEvent(
                UUID.randomUUID(), Instant.now(), "JobIntelligenceRun", runId,
                runId, jobPostingId, postingTitle, success, skillsTagged, requestedBy);
    }
}
