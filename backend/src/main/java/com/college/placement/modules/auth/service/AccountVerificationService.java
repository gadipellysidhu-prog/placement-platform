package com.college.placement.modules.auth.service;

import com.college.placement.modules.auth.config.VerificationProperties;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.VerificationToken;
import com.college.placement.modules.auth.domain.VerificationTokenType;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.notification.domain.NotificationChannel;
import com.college.placement.modules.notification.service.NotificationService;
import com.college.placement.shared.audit.service.AuditService;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.EmailVerifiedEvent;
import com.college.placement.shared.eventbus.events.PasswordResetCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the email-verification and password-reset workflows on top of the
 * generic {@link VerificationTokenService}. Verification/reset emails are sent
 * with the raw token embedded in a configuration-driven link; the raw token is
 * never persisted.
 *
 * <p>Request endpoints (send email) always succeed regardless of whether the
 * account exists, so they cannot be used to enumerate users. Verification links
 * are built from {@link VerificationProperties#getFrontendBaseUrl()} — never hardcoded.
 */
@Slf4j
@Service
public class AccountVerificationService {

    private static final String ENTITY = "AppUser";

    private final VerificationTokenService tokenService;
    private final VerificationProperties properties;
    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationService notificationService;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountVerificationService(VerificationTokenService tokenService,
                                      VerificationProperties properties,
                                      AppUserRepository userRepository,
                                      RefreshTokenRepository refreshTokenRepository,
                                      NotificationService notificationService,
                                      EventPublisher eventPublisher,
                                      PasswordEncoder passwordEncoder,
                                      AuditService auditService) {
        this.tokenService = tokenService;
        this.properties = properties;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    // ── Email verification ─────────────────────────────────────────────────

    /**
     * Sends an email-verification link if the account exists and is unverified. Never reveals existence.
     * Not transactional at this level: the token is persisted in {@code tokenService.issue} (its own
     * transaction) and the email is dispatched separately so a delivery failure cannot roll back — or
     * be distinguished from — the token issuance.
     */
    public void requestEmailVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }
            String rawToken = tokenService.issue(
                    user, VerificationTokenType.EMAIL_VERIFICATION,
                    properties.getEmailVerificationTtl(), null, null);
            String link = buildLink(properties.getEmailVerificationPath(), rawToken);
            sendQuietly(user, "Verify your email",
                    "Welcome to " + properties.getIssuer() + ".\n\n"
                    + "Please verify your email address using the link below:\n" + link
                    + "\n\nThis link expires in " + properties.getEmailVerificationTtl().toHours() + " hours.");
        });
    }

    /** Consumes an email-verification token and marks the account verified. */
    @Transactional
    public void confirmEmailVerification(String rawToken) {
        VerificationToken token = tokenService.consume(rawToken, VerificationTokenType.EMAIL_VERIFICATION);
        AppUser user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        eventPublisher.publish(EmailVerifiedEvent.of(user.getId(), user.getEmail()));
        auditService.record(ENTITY, user.getId().toString(), "EMAIL_VERIFIED");
    }

    // ── Password reset ──────────────────────────────────────────────────────

    /** Sends a password-reset link if the account exists. Never reveals existence. See {@link #requestEmailVerification}. */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = tokenService.issue(
                    user, VerificationTokenType.PASSWORD_RESET,
                    properties.getPasswordResetTtl(), null, null);
            String link = buildLink(properties.getPasswordResetPath(), rawToken);
            sendQuietly(user, "Reset your password",
                    "A password reset was requested for your " + properties.getIssuer() + " account.\n\n"
                    + "Reset your password using the link below:\n" + link
                    + "\n\nThis link expires in " + properties.getPasswordResetTtl().toMinutes() + " minutes."
                    + "\nIf you did not request this, you can safely ignore this email.");
        });
    }

    /**
     * Consumes a password-reset token, sets the new password and revokes all
     * refresh tokens so existing sessions cannot be renewed.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        VerificationToken token = tokenService.consume(rawToken, VerificationTokenType.PASSWORD_RESET);
        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
        eventPublisher.publish(PasswordResetCompletedEvent.of(user.getId(), user.getEmail()));
        auditService.record(ENTITY, user.getId().toString(), "PASSWORD_RESET");
    }

    // ── internals ────────────────────────────────────────────────────────────

    private String buildLink(String path, String rawToken) {
        return properties.getFrontendBaseUrl() + path + "?token=" + rawToken;
    }

    /** Sends an email without letting delivery failures surface (avoids leaking timing/existence). */
    private void sendQuietly(AppUser user, String subject, String body) {
        try {
            notificationService.send(user, NotificationChannel.EMAIL, subject, body);
        } catch (RuntimeException ex) {
            log.warn("VERIFICATION_EMAIL_SEND_FAILED userId={} subject={} error={}",
                    user.getId(), subject, ex.getMessage());
        }
    }
}
