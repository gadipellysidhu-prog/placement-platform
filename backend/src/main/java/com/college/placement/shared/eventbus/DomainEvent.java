package com.college.placement.shared.eventbus;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all domain events.
 * Events represent facts that have already occurred — always past-tense and immutable.
 *
 * <p>Required fields every event implementation must provide:
 * <ul>
 *   <li>{@link #eventId()}      — unique event instance ID</li>
 *   <li>{@link #occurredOn()}   — wall-clock timestamp</li>
 *   <li>{@link #aggregateType()} — name of the aggregate root (e.g. "Student", "Company")</li>
 *   <li>{@link #aggregateId()}  — UUID of the aggregate root that produced this event</li>
 * </ul>
 *
 * <p>Default methods derived from the above:
 * <ul>
 *   <li>{@link #eventType()}  — returns the simple class name; no override needed</li>
 *   <li>{@link #occurredAt()} — alias for {@link #occurredOn()}; kept for API symmetry</li>
 * </ul>
 */
public interface DomainEvent {

    /** Unique identifier for this event instance. */
    UUID eventId();

    /** Wall-clock time at which the fact occurred. */
    Instant occurredOn();

    /** Name of the aggregate root that produced this event (e.g. "Student", "JobPosting"). */
    String aggregateType();

    /** Primary key of the aggregate root that produced this event. */
    UUID aggregateId();

    /** Simple class name of the event — derived automatically; no implementation needed. */
    default String eventType() {
        return getClass().getSimpleName();
    }

    /** Alias for {@link #occurredOn()} — satisfies the {@code occurredAt} naming convention. */
    default Instant occurredAt() {
        return occurredOn();
    }
}
