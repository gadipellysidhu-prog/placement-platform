package com.college.placement.modules.auth.service;

import com.college.placement.modules.auth.config.VerificationProperties;
import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.domain.VerificationTokenType;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.notification.domain.NotificationChannel;
import com.college.placement.modules.notification.service.NotificationService;
import com.college.placement.shared.audit.service.AuditService;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.RoleAssignedEvent;
import com.college.placement.shared.eventbus.events.RoleRemovedEvent;
import com.college.placement.shared.eventbus.events.UserDisabledEvent;
import com.college.placement.shared.eventbus.events.UserEnabledEvent;
import com.college.placement.shared.eventbus.events.UserInvitedEvent;
import com.college.placement.shared.eventbus.events.UserLockedEvent;
import com.college.placement.shared.eventbus.events.UserUnlockedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Administrative identity &amp; access management: user listing/search, account
 * state transitions (enable/disable/lock/unlock), role assignment, and
 * invitation of new privileged users. Every mutation is audited and publishes a
 * domain event; the last active administrator cannot be disabled, locked or
 * demoted. Reuses the existing verification-token, notification, event and audit
 * infrastructure.
 */
@Slf4j
@Service
public class UserAdminService {

    private static final String ENTITY = "AppUser";

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenService tokenService;
    private final VerificationProperties properties;
    private final NotificationService notificationService;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserAdminService(AppUserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            VerificationTokenService tokenService,
                            VerificationProperties properties,
                            NotificationService notificationService,
                            EventPublisher eventPublisher,
                            AuditService auditService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.properties = properties;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AppUser> list(Role role, AccountStatus status, String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query;
        return userRepository.search(role, status, q, pageable);
    }

    @Transactional(readOnly = true)
    public AppUser getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // ── Account state ────────────────────────────────────────────────────────

    @Transactional
    public AppUser enable(UUID id) {
        AppUser user = getById(id);
        AccountStatus previous = user.getStatus();
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        auditService.record(ENTITY, id.toString(), "USER_ENABLED", previous.name(), AccountStatus.ACTIVE.name(), null);
        eventPublisher.publish(UserEnabledEvent.of(id, user.getEmail()));
        return user;
    }

    @Transactional
    public AppUser disable(UUID id) {
        AppUser user = getById(id);
        guardNotLastAdmin(user, "disable");
        AccountStatus previous = user.getStatus();
        user.setStatus(AccountStatus.DISABLED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
        auditService.record(ENTITY, id.toString(), "USER_DISABLED", previous.name(), AccountStatus.DISABLED.name(), null);
        eventPublisher.publish(UserDisabledEvent.of(id, user.getEmail()));
        return user;
    }

    @Transactional
    public AppUser lock(UUID id) {
        AppUser user = getById(id);
        guardNotLastAdmin(user, "lock");
        AccountStatus previous = user.getStatus();
        user.setStatus(AccountStatus.LOCKED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
        auditService.record(ENTITY, id.toString(), "USER_LOCKED", previous.name(), AccountStatus.LOCKED.name(), null);
        eventPublisher.publish(UserLockedEvent.of(id, user.getEmail()));
        return user;
    }

    @Transactional
    public AppUser unlock(UUID id) {
        AppUser user = getById(id);
        AccountStatus previous = user.getStatus();
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        auditService.record(ENTITY, id.toString(), "USER_UNLOCKED", previous.name(), AccountStatus.ACTIVE.name(), null);
        eventPublisher.publish(UserUnlockedEvent.of(id, user.getEmail()));
        return user;
    }

    // ── Roles ────────────────────────────────────────────────────────────────

    @Transactional
    public AppUser assignRole(UUID id, Role newRole) {
        AppUser user = getById(id);
        Role previous = user.getRole();
        if (previous == newRole) {
            return user;
        }
        // Demoting the final active admin would lock out administration.
        if (previous == Role.ROLE_ADMIN) {
            guardNotLastAdmin(user, "change role");
        }
        user.setRole(newRole);
        userRepository.save(user);
        auditService.record(ENTITY, id.toString(), "ROLE_CHANGED", previous.name(), newRole.name(), null);
        eventPublisher.publish(RoleRemovedEvent.of(id, user.getEmail(), previous.name()));
        eventPublisher.publish(RoleAssignedEvent.of(id, user.getEmail(), newRole.name()));
        return user;
    }

    // ── Invitations ──────────────────────────────────────────────────────────

    @Transactional
    public AppUser invite(String email, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        // Unusable random password until the invitee sets their own on acceptance.
        user.setPasswordHash(passwordEncoder.encode(randomSecret()));
        user.setRole(role);
        user.setStatus(AccountStatus.INVITED);
        user.setEmailVerified(false);
        userRepository.save(user);

        String rawToken = tokenService.issue(
                user, VerificationTokenType.USER_INVITATION, properties.getInvitationTtl(), actor(), role.name());
        String link = properties.getFrontendBaseUrl() + properties.getInvitationPath() + "?token=" + rawToken;
        sendQuietly(user, "You have been invited to " + properties.getIssuer(),
                "You have been invited to join " + properties.getIssuer() + " as " + role.name() + ".\n\n"
                + "Accept the invitation and set your password using the link below:\n" + link
                + "\n\nThis invitation expires in " + properties.getInvitationTtl().toHours() + " hours.");

        auditService.record(ENTITY, user.getId().toString(), "USER_INVITED", null, role.name(), "email=" + email);
        eventPublisher.publish(UserInvitedEvent.of(user.getId(), email, role.name()));
        return user;
    }

    // ── internals ────────────────────────────────────────────────────────────

    /** Blocks an action that would remove the final active administrator. */
    private void guardNotLastAdmin(AppUser user, String action) {
        if (user.getRole() == Role.ROLE_ADMIN && user.getStatus() == AccountStatus.ACTIVE
                && userRepository.countByRoleAndStatus(Role.ROLE_ADMIN, AccountStatus.ACTIVE) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot " + action + " the last active administrator.");
        }
    }

    private String actor() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendQuietly(AppUser user, String subject, String body) {
        try {
            notificationService.send(user, NotificationChannel.EMAIL, subject, body);
        } catch (RuntimeException ex) {
            log.warn("INVITATION_EMAIL_SEND_FAILED userId={} error={}", user.getId(), ex.getMessage());
        }
    }
}
