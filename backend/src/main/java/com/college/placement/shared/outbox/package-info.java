/**
 * Transactional outbox: schema, service, poller, and cleanup.
 *
 * <p>Guarantees at-least-once delivery of side-effects using SELECT ... FOR UPDATE SKIP LOCKED
 * (SADD 5, MOP 16).
 */
package com.college.placement.shared.outbox;
