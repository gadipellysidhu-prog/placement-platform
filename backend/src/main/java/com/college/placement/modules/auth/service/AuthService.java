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
import com.college.placement.shared.security.JwtProperties;
import com.college.placement.shared.security.JwtService;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder,
                       EventPublisher eventPublisher) {
        this.userRepository        = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService            = jwtService;
        this.jwtProperties         = jwtProperties;
        this.passwordEncoder       = passwordEncoder;
        this.eventPublisher        = eventPublisher;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
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
        return tokens;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(AuthException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }

        if (user.isAccountLocked()) {
            throw AuthException.accountLocked();
        }

        refreshTokenRepository.revokeAllByUser(user);
        return issueTokens(user);
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
