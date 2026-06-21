package com.college.placement.shared.eventbus;

/**
 * Abstraction over the underlying event dispatch mechanism.
 * Services depend on this interface, not on Spring's ApplicationEventPublisher directly,
 * keeping the event publishing contract testable and implementation-agnostic.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
