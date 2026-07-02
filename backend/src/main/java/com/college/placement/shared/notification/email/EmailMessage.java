package com.college.placement.shared.notification.email;

/**
 * A single outbound email. Plain-text body for now; HTML templating is added by
 * the notification template engine in a later phase.
 *
 * @param to      recipient address
 * @param subject subject line
 * @param body    plain-text body
 */
public record EmailMessage(String to, String subject, String body) {
}
