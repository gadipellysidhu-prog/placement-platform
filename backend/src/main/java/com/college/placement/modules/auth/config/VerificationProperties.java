package com.college.placement.modules.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;

/**
 * Configuration for the verification-token subsystem. All token lifetimes,
 * lengths and link construction are driven from here — no hardcoded values in
 * business code.
 */
@ConfigurationProperties(prefix = "auth.verification")
@Getter
@Setter
public class VerificationProperties {

    /** Number of random bytes per raw token (before base64url encoding). */
    private int tokenBytes = 32;

    /** Lifetime of an email-verification token. */
    private Duration emailVerificationTtl = Duration.ofHours(24);

    /** Lifetime of a password-reset token. */
    private Duration passwordResetTtl = Duration.ofHours(1);

    /** Lifetime of a user-invitation token. */
    private Duration invitationTtl = Duration.ofHours(72);

    /** Base URL of the frontend that hosts the verification/reset pages. */
    private String frontendBaseUrl = "http://localhost:5173";

    /** Frontend path that consumes an email-verification token. */
    private String emailVerificationPath = "/verify-email";

    /** Frontend path that consumes a password-reset token. */
    private String passwordResetPath = "/reset-password";

    /** Frontend path that consumes a user-invitation token. */
    private String invitationPath = "/accept-invitation";

    /** Human-readable issuer name used in email copy. */
    private String issuer = "Placement Platform";

    @NestedConfigurationProperty
    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Cleanup {
        /** Whether the scheduled cleanup job runs. */
        private boolean enabled = true;
        /** Fixed delay between cleanup runs, in milliseconds. */
        private long intervalMs = 3_600_000;
    }
}
