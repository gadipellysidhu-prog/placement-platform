package com.college.placement.modules.jobintelligence.events;

import com.college.placement.shared.eventbus.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an officer starts (or retries) an AI extraction run. Carries only
 * what the pipeline needs to begin; the async listener does the heavy lifting.
 */
public record JobIntelligenceRequestedEvent(
        UUID eventId,
        Instant occurredOn,
        String aggregateType,
        UUID aggregateId,
        UUID runId,
        UUID jobPostingId,
        String officialUrl
) implements DomainEvent {

    public static JobIntelligenceRequestedEvent of(UUID runId, UUID jobPostingId, String officialUrl) {
        return new JobIntelligenceRequestedEvent(
                UUID.randomUUID(), Instant.now(), "JobIntelligenceRun", runId,
                runId, jobPostingId, officialUrl);
    }
}
