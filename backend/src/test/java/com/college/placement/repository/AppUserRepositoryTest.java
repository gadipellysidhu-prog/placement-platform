package com.college.placement.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AppUserRepositoryTest {

    @Autowired AppUserRepository userRepository;
    @Autowired TestEntityManager em;

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    void findByEmail_existingUser_returnsUser() {
        AppUser user = buildUser("alice@test.com", Role.ROLE_STUDENT);
        em.persistAndFlush(user);

        Optional<AppUser> result = userRepository.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
        assertThat(result.get().getRole()).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void findByEmail_nonExistingUser_returnsEmpty() {
        Optional<AppUser> result = userRepository.findByEmail("nobody@test.com");

        assertThat(result).isEmpty();
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    void existsByEmail_whenPresent_returnsTrue() {
        em.persistAndFlush(buildUser("bob@test.com", Role.ROLE_PLACEMENT_OFFICER));

        assertThat(userRepository.existsByEmail("bob@test.com")).isTrue();
    }

    @Test
    void existsByEmail_whenAbsent_returnsFalse() {
        assertThat(userRepository.existsByEmail("ghost@test.com")).isFalse();
    }

    // ── unique constraint ─────────────────────────────────────────────────────

    @Test
    void save_duplicateEmail_throwsConstraintViolation() {
        em.persistAndFlush(buildUser("dup@test.com", Role.ROLE_STUDENT));

        AppUser duplicate = buildUser("dup@test.com", Role.ROLE_ADMIN);

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── brute-force fields ────────────────────────────────────────────────────

    @Test
    void save_bruteForceFields_persistCorrectly() {
        AppUser user = buildUser("carol@test.com", Role.ROLE_STUDENT);
        Instant lockUntil = Instant.now().plusSeconds(900);
        user.setFailureCount(5);
        user.setLastFailureAt(Instant.now());
        user.setLockedUntil(lockUntil);

        AppUser saved = userRepository.saveAndFlush(user);
        em.clear();

        AppUser reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFailureCount()).isEqualTo(5);
        assertThat(reloaded.getLastFailureAt()).isNotNull();
        assertThat(reloaded.getLockedUntil()).isNotNull();
        assertThat(reloaded.getLockedUntil()).isAfter(Instant.now());
    }

    // ── version / optimistic locking ──────────────────────────────────────────

    @Test
    void save_newUser_versionStartsAtZero() {
        AppUser saved = userRepository.saveAndFlush(buildUser("ver@test.com", Role.ROLE_ADMIN));

        assertThat(saved.getVersion()).isZero();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static AppUser buildUser(String email, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$hashedpassword");
        u.setRole(role);
        return u;
    }
}
