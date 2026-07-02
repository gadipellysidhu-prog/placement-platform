package com.college.placement.shared.notification.email;

/**
 * Raised when email delivery is enabled but the transport fails. Propagated to
 * the transactional outbox so the send is retried with exponential backoff.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
