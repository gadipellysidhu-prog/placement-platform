/**
 * Notification module: email sending, driven by the transactional outbox.
 *
 * <p>Business services NEVER call this module directly; they write outbox events that the poller
 * delivers asynchronously. Handlers must be idempotent (SADD 5, MOP 3.4).
 */
package com.college.placement.modules.notification;
