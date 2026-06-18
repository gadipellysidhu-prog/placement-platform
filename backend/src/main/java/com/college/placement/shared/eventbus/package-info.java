/**
 * Internal event bus: configuration and the documented event registry.
 *
 * <p>Modules publish domain events via Spring's ApplicationEventPublisher; subscribers use
 * {@code @TransactionalEventListener(AFTER_COMMIT)} (SADD 4, MOP 15).
 */
package com.college.placement.shared.eventbus;
