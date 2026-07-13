package com.college.placement.auth;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.domain.VerificationToken;
import com.college.placement.modules.auth.domain.VerificationTokenType;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.auth.repository.VerificationTokenRepository;
import com.college.placement.support.DatabaseCleaner;
import com.college.placement.modules.auth.service.VerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.college.placement.Application.class)
@ActiveProfiles("test")
class VerificationTokenServiceTest {

    @Autowired VerificationTokenService tokenService;
    @Autowired VerificationTokenRepository tokenRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired DatabaseCleaner databaseCleaner;

    private AppUser user;

    @BeforeEach
    void setUp() {
        // Clear user-dependent rows left by other tests in the shared context before
        // deleting users (verification_tokens/notification_history cascade; refresh_tokens do not).
        tokenRepository.deleteAll();
        databaseCleaner.clean();
        AppUser u = new AppUser();
        u.setEmail("token-user@test.com");
        u.setPasswordHash("hash");
        u.setRole(Role.ROLE_STUDENT);
        user = userRepository.save(u);
    }

    @Test
    void issue_returnsRawToken_andStoresOnlyHash() {
        String raw = tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);

        assertThat(raw).isNotBlank();
        // Raw token is never stored verbatim.
        assertThat(tokenRepository.findByTokenHash(raw)).isEmpty();
        VerificationToken stored = tokenRepository.findAll().get(0);
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(raw);
        assertThat(stored.isConsumed()).isFalse();
    }

    @Test
    void consume_validToken_marksConsumed_andReturnsUser() {
        String raw = tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);

        VerificationToken consumed = tokenService.consume(raw, VerificationTokenType.EMAIL_VERIFICATION);

        assertThat(consumed.getUser().getId()).isEqualTo(user.getId());
        assertThat(tokenRepository.findAll().get(0).isConsumed()).isTrue();
    }

    @Test
    void consume_wrongType_throws() {
        String raw = tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);

        assertThatThrownBy(() -> tokenService.consume(raw, VerificationTokenType.PASSWORD_RESET))
                .isInstanceOf(ResponseStatusException.class);
        // Not consumed by the failed attempt.
        assertThat(tokenRepository.findAll().get(0).isConsumed()).isFalse();
    }

    @Test
    void consume_expiredToken_throws() {
        String raw = tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);
        VerificationToken t = tokenRepository.findAll().get(0);
        t.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        tokenRepository.saveAndFlush(t);

        assertThatThrownBy(() -> tokenService.consume(raw, VerificationTokenType.EMAIL_VERIFICATION))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void consume_twice_replayIsRejected() {
        String raw = tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);

        tokenService.consume(raw, VerificationTokenType.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> tokenService.consume(raw, VerificationTokenType.EMAIL_VERIFICATION))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void issue_revokesPreviousActiveTokenOfSameType() {
        String firstRaw = tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);
        tokenService.issue(user, VerificationTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(1), null, null);

        // The superseded token can no longer be consumed.
        assertThatThrownBy(() -> tokenService.consume(firstRaw, VerificationTokenType.EMAIL_VERIFICATION))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void consume_invalidToken_throws() {
        assertThatThrownBy(() -> tokenService.consume("not-a-real-token", VerificationTokenType.EMAIL_VERIFICATION))
                .isInstanceOf(ResponseStatusException.class);
    }
}
