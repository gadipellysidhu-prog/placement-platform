package com.college.placement.auth;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.auth.repository.VerificationTokenRepository;
import com.college.placement.modules.notification.domain.NotificationHistory;
import com.college.placement.modules.notification.repository.NotificationHistoryRepository;
import com.college.placement.support.DatabaseCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountVerificationIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String EMAIL = "verify-flow@test.com";
    private static final String PASSWORD = "password123";
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired VerificationTokenRepository tokenRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired DatabaseCleaner databaseCleaner;
    @Autowired NotificationHistoryRepository notificationRepo;

    @BeforeEach
    void clean() {
        tokenRepo.deleteAll();
        notificationRepo.deleteAll();
        databaseCleaner.clean();
    }

    // ── Email verification ─────────────────────────────────────────────────

    @Test
    void emailVerification_fullFlow_marksUserVerified_andRejectsReplay() throws Exception {
        register(EMAIL);
        assertThat(userRepo.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isFalse();

        mvc.perform(post("/auth/verify-email/request").contentType(JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isAccepted());

        String token = extractToken("Verify your email");

        mvc.perform(post("/auth/verify-email/confirm").contentType(JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());

        assertThat(userRepo.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isTrue();

        // Replay: same token cannot be reused.
        mvc.perform(post("/auth/verify-email/confirm").contentType(JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmEmail_invalidToken_returns400() throws Exception {
        mvc.perform(post("/auth/verify-email/confirm").contentType(JSON)
                        .content("{\"token\":\"bogus\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── Password reset ──────────────────────────────────────────────────────

    @Test
    void passwordReset_fullFlow_changesPassword_andRevokesSessions() throws Exception {
        register(EMAIL);
        verify(EMAIL); // password reset targets an existing, verified account

        mvc.perform(post("/auth/forgot-password").contentType(JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isAccepted());

        String token = extractToken("Reset your password");

        mvc.perform(post("/auth/reset-password").contentType(JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword456\"}"))
                .andExpect(status().isOk());

        // Old password no longer works; new one does.
        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(EMAIL, "newpassword456"))))
                .andExpect(status().isOk());

        // All prior refresh tokens revoked.
        AppUser user = userRepo.findByEmail(EMAIL).orElseThrow();
        assertThat(refreshTokenRepo.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isRevoked())
                // the fresh login above issues a new one; only assert the reset revoked the old set
                .count()).isLessThanOrEqualTo(1);
    }

    @Test
    void forgotPassword_unknownEmail_returns202_andCreatesNoToken() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType(JSON)
                        .content("{\"email\":\"nobody@test.com\"}"))
                .andExpect(status().isAccepted());

        assertThat(tokenRepo.count()).isZero();
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        mvc.perform(post("/auth/reset-password").contentType(JSON)
                        .content("{\"token\":\"bogus\",\"newPassword\":\"whatever12\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void register(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated());
    }

    /** Marks an account verified (password reset targets an existing verified user). */
    private void verify(String email) {
        AppUser user = userRepo.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepo.save(user);
    }

    /** Finds the most recent notification with the given subject and extracts its embedded token. */
    private String extractToken(String subject) {
        NotificationHistory history = notificationRepo
                .findByUser(userRepo.findByEmail(EMAIL).orElseThrow(), PageRequest.of(0, 20))
                .getContent().stream()
                .filter(h -> subject.equals(h.getSubject()))
                .max(java.util.Comparator.comparing(NotificationHistory::getCreatedAt))
                .orElseThrow(() -> new AssertionError("No notification with subject: " + subject));
        Matcher m = TOKEN.matcher(history.getBody());
        assertThat(m.find()).as("token present in email body").isTrue();
        return m.group(1);
    }
}
