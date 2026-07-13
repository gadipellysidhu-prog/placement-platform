package com.college.placement.shared.devtools;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds ready-to-use demo accounts so developers can log in immediately.
 *
 * <p>Active only under the {@code dev} profile ({@code mvn spring-boot:run}) and
 * therefore never runs in {@code prod}. Public registration is restricted to
 * {@code ROLE_STUDENT} and no accounts are seeded by migrations, so without this
 * there is no way to obtain an administrator login on a fresh database.
 *
 * <p>Idempotent: each account is created only when its email is absent, so
 * restarts and re-runs never produce duplicates. Passwords are hashed with the
 * real {@link PasswordEncoder} bean (BCrypt), exactly as the auth flow expects.
 */
@Slf4j
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@placement.local";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String STUDENT_EMAIL = "student@placement.local";
    private static final String STUDENT_PASSWORD = "Student@123";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ROLE_ADMIN);
        seedUser(STUDENT_EMAIL, STUDENT_PASSWORD, Role.ROLE_STUDENT);
    }

    /**
     * Creates a login-ready {@link AppUser} if one does not already exist for the
     * given email. The account is ACTIVE and email-verified so it satisfies every
     * gate enforced by the login flow.
     */
    private void seedUser(String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            log.info("DEV_SEED event=SKIP_EXISTING email={} role={}", email, role);
            return;
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("DEV_SEED event=CREATED email={} role={} status=ACTIVE emailVerified=true", email, role);
    }
}
