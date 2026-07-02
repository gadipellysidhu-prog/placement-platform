package com.college.placement.shared.notification.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for outbound notification email. When {@code enabled} is false
 * the provider logs and skips delivery — the default in test and any environment
 * without an SMTP relay configured.
 */
@ConfigurationProperties(prefix = "notification.email")
@Getter
@Setter
public class NotificationEmailProperties {

    /** Whether email is actually dispatched. Disabled by default. */
    private boolean enabled = false;

    /** From address applied to every outbound email. */
    private String from = "no-reply@placement.local";
}
