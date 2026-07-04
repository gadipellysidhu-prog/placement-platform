package com.college.placement.auth;

import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.auth.repository.VerificationTokenRepository;
import com.college.placement.modules.notification.domain.NotificationHistory;
import com.college.placement.modules.notification.repository.NotificationHistoryRepository;
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

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase C — email-verification enforcement: registration creates unverified
 * accounts with no JWT, unverified login is rejected, verification unlocks login,
 * and resend issues a fresh token while revoking the previous one.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailVerificationEnforcementTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String EMAIL = "enforce@test.com";
    private static final String PASSWORD = "password123";
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired VerificationTokenRepository tokenRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired NotificationHistoryRepository notificationRepo;

    @BeforeEach
    void clean() {
        tokenRepo.deleteAll();
        notificationRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void registration_createsUnverifiedAccount_withoutJwt() throws Exception {
        mvc.perform(post("/auth/register").contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        assertThat(userRepo.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void login_unverified_returns403_andIssuesNoJwt() throws Exception {
        register(EMAIL);

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void verifyThenLogin_succeeds() throws Exception {
        register(EMAIL);
        confirm(latestToken("Verify your email"));

        assertThat(userRepo.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isTrue();

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void resendVerification_returns202_issuesNewToken_andRevokesPrevious() throws Exception {
        register(EMAIL);
        String firstToken = latestToken("Verify your email");

        mvc.perform(post("/auth/resend-verification").contentType(JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String secondToken = latestToken("Verify your email");
        assertThat(secondToken).isNotEqualTo(firstToken);

        // Previous token is revoked; only the newest is valid.
        confirm(firstToken).andExpect(status().isBadRequest());
        confirm(secondToken).andExpect(status().isOk());
    }

    @Test
    void resendVerification_unknownEmail_returns202_generic_withNoTokenCreated() throws Exception {
        mvc.perform(post("/auth/resend-verification").contentType(JSON)
                        .content("{\"email\":\"ghost@test.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(tokenRepo.count()).isZero();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void register(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token) throws Exception {
        return mvc.perform(post("/auth/verify-email/confirm").contentType(JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    private String latestToken(String subject) {
        NotificationHistory history = notificationRepo
                .findByUser(userRepo.findByEmail(EMAIL).orElseThrow(), PageRequest.of(0, 20))
                .getContent().stream()
                .filter(h -> subject.equals(h.getSubject()))
                .max(Comparator.comparing(NotificationHistory::getCreatedAt))
                .orElseThrow(() -> new AssertionError("No notification with subject: " + subject));
        Matcher m = TOKEN.matcher(history.getBody());
        assertThat(m.find()).as("token present in email body").isTrue();
        return m.group(1);
    }
}
