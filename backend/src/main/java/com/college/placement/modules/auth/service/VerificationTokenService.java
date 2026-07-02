package com.college.placement.modules.auth.service;

import com.college.placement.modules.auth.config.VerificationProperties;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.VerificationToken;
import com.college.placement.modules.auth.domain.VerificationTokenType;
import com.college.placement.modules.auth.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generic, reusable verification-token engine: generation, hashing, validation,
 * one-time consumption and scheduled cleanup. Not tied to any single workflow —
 * email verification, password reset and invitations all use it via
 * {@link VerificationTokenType}.
 *
 * <p>Security: raw tokens are produced with {@link SecureRandom} and only their
 * SHA-256 hash is persisted. Validation failures all raise the same generic
 * error so callers never leak which specific check failed (or whether a token or
 * user exists). Consumption is race-free via a conditional UPDATE.
 */
@Slf4j
@Service
public class VerificationTokenService {

    /** Uniform, non-revealing failure raised for every invalid/expired/used token. */
    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification token");
    }

    private final VerificationTokenRepository repository;
    private final VerificationProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationTokenService(VerificationTokenRepository repository,
                                    VerificationProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Issues a fresh token of the given type for the user, revoking any previous
     * active tokens of the same type (only the latest is valid). Returns the
     * <strong>raw</strong> token — the only place it ever exists — for embedding
     * in a link; only its hash is stored.
     */
    @Transactional
    public String issue(AppUser user, VerificationTokenType type, Duration ttl,
                        String createdBy, String metadata) {
        repository.revokeActive(user, type);

        String rawToken = generateRawToken();
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setTokenHash(sha256Hex(rawToken));
        token.setType(type);
        token.setExpiresAt(Instant.now().plus(ttl));
        token.setCreatedBy(createdBy);
        token.setMetadata(metadata);
        repository.save(token);
        return rawToken;
    }

    /**
     * Validates and atomically consumes a raw token, returning the persisted
     * token (with its user) on success. Every failure mode raises the same
     * generic error to avoid leaking information.
     */
    @Transactional
    public VerificationToken consume(String rawToken, VerificationTokenType expectedType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }
        String hash = sha256Hex(rawToken);
        VerificationToken token = repository.findByTokenHash(hash).orElseThrow(VerificationTokenService::invalidToken);

        // Constant-time comparison as defence in depth (lookup is already by exact hash).
        if (!MessageDigest.isEqual(
                hash.getBytes(StandardCharsets.UTF_8),
                token.getTokenHash().getBytes(StandardCharsets.UTF_8))) {
            throw invalidToken();
        }
        if (token.getType() != expectedType || token.isRevoked() || token.isConsumed()
                || token.getExpiresAt().isBefore(Instant.now())) {
            throw invalidToken();
        }

        // Eagerly touch the user while attached (EAGER, so already loaded) before the
        // conditional update clears the persistence context.
        token.getUser().getEmail();

        int updated = repository.markConsumed(token.getId(), Instant.now());
        if (updated == 0) {
            // Lost the race — another request consumed it first (replay protection).
            throw invalidToken();
        }
        return token;
    }

    /** Scheduled removal of expired, consumed and revoked tokens. */
    @Scheduled(fixedDelayString = "${auth.verification.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupExpired() {
        if (!properties.getCleanup().isEnabled()) {
            return;
        }
        int deleted = repository.deleteExpiredAndUsed(Instant.now());
        if (deleted > 0) {
            log.info("VERIFICATION_TOKEN_CLEANUP deleted={}", deleted);
        }
    }

    // ── internals ────────────────────────────────────────────────────────────

    private String generateRawToken() {
        byte[] bytes = new byte[properties.getTokenBytes()];
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
