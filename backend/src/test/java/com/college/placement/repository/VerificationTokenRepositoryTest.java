package com.college.placement.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.domain.VerificationToken;
import com.college.placement.modules.auth.domain.VerificationTokenType;
import com.college.placement.modules.auth.repository.VerificationTokenRepository;
import com.college.placement.shared.audit.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class VerificationTokenRepositoryTest {

    @Autowired VerificationTokenRepository repository;
    @Autowired TestEntityManager em;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(buildUser("verify@test.com"));
    }

    @Test
    void findByTokenHash_returnsToken() {
        em.persistAndFlush(buildToken("hash-a", VerificationTokenType.EMAIL_VERIFICATION, future()));

        Optional<VerificationToken> found = repository.findByTokenHash("hash-a");

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(VerificationTokenType.EMAIL_VERIFICATION);
    }

    @Test
    void markConsumed_firstCallUpdates_secondCallIsNoOp() {
        VerificationToken token = em.persistAndFlush(
                buildToken("hash-b", VerificationTokenType.EMAIL_VERIFICATION, future()));

        int first = repository.markConsumed(token.getId(), Instant.now());
        int second = repository.markConsumed(token.getId(), Instant.now());

        assertThat(first).isEqualTo(1);   // winning consumer
        assertThat(second).isZero();      // replay / concurrent loser
    }

    @Test
    void revokeActive_revokesOnlyMatchingTypeAndUnused() {
        em.persistAndFlush(buildToken("hash-c", VerificationTokenType.EMAIL_VERIFICATION, future()));
        em.persistAndFlush(buildToken("hash-d", VerificationTokenType.PASSWORD_RESET, future()));

        repository.revokeActive(user, VerificationTokenType.EMAIL_VERIFICATION);
        em.clear();

        assertThat(repository.findByTokenHash("hash-c").orElseThrow().isRevoked()).isTrue();
        assertThat(repository.findByTokenHash("hash-d").orElseThrow().isRevoked()).isFalse();
    }

    @Test
    void deleteExpiredAndUsed_removesExpiredAndConsumed_keepsActive() {
        em.persistAndFlush(buildToken("hash-active", VerificationTokenType.EMAIL_VERIFICATION, future()));
        em.persistAndFlush(buildToken("hash-expired", VerificationTokenType.EMAIL_VERIFICATION, past()));
        VerificationToken consumed = buildToken("hash-consumed", VerificationTokenType.EMAIL_VERIFICATION, future());
        consumed.setConsumed(true);
        em.persistAndFlush(consumed);

        int deleted = repository.deleteExpiredAndUsed(Instant.now());
        em.clear();

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findByTokenHash("hash-active")).isPresent();
        assertThat(repository.findByTokenHash("hash-expired")).isEmpty();
        assertThat(repository.findByTokenHash("hash-consumed")).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Instant future() {
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

    private static Instant past() {
        return Instant.now().minus(1, ChronoUnit.HOURS);
    }

    private AppUser buildUser(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setRole(Role.ROLE_STUDENT);
        return u;
    }

    private VerificationToken buildToken(String hash, VerificationTokenType type, Instant expiresAt) {
        VerificationToken t = new VerificationToken();
        t.setUser(user);
        t.setTokenHash(hash);
        t.setType(type);
        t.setExpiresAt(expiresAt);
        return t;
    }
}
