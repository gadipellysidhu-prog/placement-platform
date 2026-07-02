package com.college.placement.shared.notification.email;

/**
 * Abstraction over the active email transport. Business code and the outbox
 * notification handler depend only on this interface, so the transport (SMTP,
 * and future SES / SendGrid / Mailgun adapters) can be swapped via configuration.
 *
 * <p>Implementations throw {@link EmailDeliveryException} on a delivery failure so
 * the caller (typically the transactional outbox) can retry with backoff.
 */
public interface EmailProvider {

    /**
     * Sends the message. Implementations may no-op (and log) when email delivery
     * is disabled by configuration, in which case this returns normally.
     *
     * @throws EmailDeliveryException if delivery is enabled but fails
     */
    void send(EmailMessage message);

    /** Stable identifier of the active provider (e.g. {@code smtp}). */
    String providerId();
}
