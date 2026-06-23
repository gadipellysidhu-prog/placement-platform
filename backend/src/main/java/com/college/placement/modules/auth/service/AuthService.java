package com.college.placement.modules.auth.service;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.RefreshToken;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.UserRegisteredEvent;
import com.college.placement.shared.exception.AuthException;
import com.college.placement.shared.observability.metrics.AuthMetrics;
import com.college.placement.shared.security.SecurityAuditLogger;
import com.college.placement.shared.security.SecurityProperties;
import com.college.placement.shared.security.JwtProperties;
import com.college.placement.shared.security.JwtService;
import io.micrometer.core.instrument.Timer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final AuthMetrics authMetrics;
    private final SecurityProperties securityProperties;
    private final SecurityAuditLogger auditLogger;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder,
                       EventPublisher eventPublisher,
                       AuthMetrics authMetrics,
                       SecurityProperties securityProperties,
                       SecurityAuditLogger auditLogger) {
        this.userRepository       = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService           = jwtService;
        this.jwtProperties        = jwtProperties;
        this.passwordEncoder      = passwordEncoder;
        this.eventPublisher       = eventPublisher;
        this.authMetrics          = authMetrics;
        this.securityProperties   = securityProperties;
        this.auditLogger          = auditLogger;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        Timer.Sample sample = authMetrics.startSample();
        try {
            if (userRepository.existsByEmail(request.email())) {
                throw AuthException.emailAlreadyRegistered();
            }

            AppUser user = new AppUser();
            user.setEmail(request.email());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setRole(request.role());
            userRepository.save(user);

            TokenResponse tokens = issueTokens(user);
            eventPublisher.publish(UserRegisteredEvent.of(user.getId(), user.getEmail(), user.getRole().name()));
            authMetrics.recordRegisterSuccess();
            return tokens;
        } finally {
            authMetrics.stopRegisterTimer(sample);
        }
    }

    @Transactional(noRollbackFor = AuthException.class)
    public TokenResponse login(LoginRequest request) {
        Timer.Sample sample = authMetrics.startSample();
        try {
            AppUser user = userRepository.findByEmail(request.email())
                    .orElseThrow(AuthException::invalidCredentials);

            checkBruteForceLock(user);

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                recordLoginFailure(user);
                authMetrics.recordLoginFailure();
                throw AuthException.invalidCredentials();
            }

            if (user.isAccountLocked()) {
                authMetrics.recordLoginFailure();
                throw AuthException.accountLocked();
            }

            resetLoginFailures(user);
            refreshTokenRepository.revokeAllByUser(user);
            TokenResponse tokens = issueTokens(user);
            auditLogger.loginSuccess(user.getEmail(), "");
            authMetrics.recordLoginSuccess();
            return tokens;
        } catch (AuthException ex) {
            throw ex;
        } finally {
            authMetrics.stopLoginTimer(sample);
        }
    }

    @Transactional
    public TokenResponse refresh(String rawToken) {
        String hash = sha256Hex(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(AuthException::invalidRefreshToken);

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw AuthException.invalidRefreshToken();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        String hash = sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    public void initiateEmailVerification(String email) {
        // Placeholder: Phase 6 outbox will dispatch verification email.
    }

    public void initiateForgotPassword(String email) {
        // Placeholder: Phase 6 outbox will dispatch password reset email.
    }

    // ── Brute-force protection ────────────────────────────────────────────────

    private void checkBruteForceLock(AppUser user) {
        Instant lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            auditLogger.accountLocked(user.getEmail(), "");
            throw AuthException.accountLocked();
        }
        if (lockedUntil != null && lockedUntil.isBefore(Instant.now())) {
            user.setLockedUntil(null);
            user.setFailureCount(0);
            user.setLastFailureAt(null);
            userRepository.save(user);
        }
    }

    private void recordLoginFailure(AppUser user) {
        int failures = user.getFailureCount() + 1;
        user.setFailureCount(failures);
        user.setLastFailureAt(Instant.now());

        int maxFailures = securityProperties.getLockout().getMaxFailures();
        if (failures >= maxFailures) {
            int lockMinutes = securityProperties.getLockout().getLockDurationMinutes();
            user.setLockedUntil(Instant.now().plusSeconds(lockMinutes * 60L));
            auditLogger.accountLocked(user.getEmail(), "");
        } else {
            auditLogger.loginFailure(user.getEmail(), "", "bad_credentials attempt=" + failures);
        }

        userRepository.save(user);
    }

    private void resetLoginFailures(AppUser user) {
        if (user.getFailureCount() > 0 || user.getLockedUntil() != null) {
            user.setFailureCount(0);
            user.setLastFailureAt(null);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    // ── Token issuance ────────────────────────────────────────────────────────

    private TokenResponse issueTokens(AppUser user) {
        String accessToken  = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String rawRefresh   = generateSecureToken();

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(sha256Hex(rawRefresh));
        rt.setExpiresAt(Instant.now().plusMillis(jwtProperties.refreshTokenExpiryMs()));
        refreshTokenRepository.save(rt);

        return TokenResponse.of(accessToken, rawRefresh, jwtProperties.accessTokenExpiryMs());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
