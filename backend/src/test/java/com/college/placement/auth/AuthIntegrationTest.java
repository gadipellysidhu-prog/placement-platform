package com.college.placement.auth;

import com.college.placement.modules.auth.domain.RefreshToken;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RefreshRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests with real per-request transactions (no class-level @Transactional).
 * This mirrors production behaviour: each MockMvc call commits or rolls back independently.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private static final String STUDENT_EMAIL   = "student@test.com";
    private static final String OFFICER_EMAIL   = "officer@test.com";
    private static final String ADMIN_EMAIL     = "admin@test.com";
    private static final String TEST_PASSWORD   = "password123";
    private static final String JSON            = MediaType.APPLICATION_JSON_VALUE;
    private static final String PROBLEM_JSON    = "application/problem+json";

    @BeforeEach
    void cleanDb() {
        // child first to respect FK constraint
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_validStudent_returns201WithTokens() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(toJson(new RegisterRequest(STUDENT_EMAIL, TEST_PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiresIn").isNumber());
    }

    @Test
    void register_duplicateEmail_returns409ProblemDetail() throws Exception {
        register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(toJson(new RegisterRequest(STUDENT_EMAIL, TEST_PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Email is already registered."));
    }

    @Test
    void register_invalidEmail_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(toJson(new RegisterRequest("not-an-email", TEST_PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(toJson(new RegisterRequest(STUDENT_EMAIL, "short", Role.ROLE_STUDENT))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(STUDENT_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401ProblemDetail() throws Exception {
        register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(STUDENT_EMAIL, "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest("ghost@nowhere.com", TEST_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void login_revokesOldTokens() throws Exception {
        TokenResponse first = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        // Login again — must revoke all previous refresh tokens
        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(STUDENT_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk());

        // Old refresh token from register must now be invalid
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(first.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returns200WithNewDistinctTokens() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        MvcResult result = mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        TokenResponse next = fromJson(result, TokenResponse.class);
        assertThat(next.accessToken()).isNotEqualTo(tokens.accessToken());
        assertThat(next.refreshToken()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void refresh_unknownToken_returns401ProblemDetail() throws Exception {
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest("completely-unknown-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void refresh_revokedToken_returns401() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        // First use — rotates the token; original is now revoked
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isOk());

        // Re-use of the original revoked token must be 401
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void refresh_expiredToken_returns401() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        // Manually expire the token via the repository (direct DB manipulation)
        String hash = sha256Hex(tokens.refreshToken());
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        rt.setExpiresAt(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(rt);

        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void refresh_multipleRotations_eachSucceeds_oldTokensRevoked() throws Exception {
        TokenResponse t1 = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        TokenResponse t2 = fromJson(
                mvc.perform(post("/auth/refresh")
                                .contentType(JSON)
                                .content(toJson(new RefreshRequest(t1.refreshToken()))))
                        .andExpect(status().isOk()).andReturn(),
                TokenResponse.class);

        TokenResponse t3 = fromJson(
                mvc.perform(post("/auth/refresh")
                                .contentType(JSON)
                                .content(toJson(new RefreshRequest(t2.refreshToken()))))
                        .andExpect(status().isOk()).andReturn(),
                TokenResponse.class);

        // All three tokens are distinct
        assertThat(t1.refreshToken()).isNotEqualTo(t2.refreshToken());
        assertThat(t2.refreshToken()).isNotEqualTo(t3.refreshToken());

        // t1 and t2 are now revoked
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(t1.refreshToken()))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(t2.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_validTokens_returns204AndRevokesRefreshToken() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(post("/auth/logout")
                        .contentType(JSON)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .content(toJson(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isNoContent());

        // Refresh must now fail — token is revoked
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withoutAccessToken_returns401ProblemDetail() throws Exception {
        mvc.perform(post("/auth/logout")
                        .contentType(JSON)
                        .content(toJson(new RefreshRequest("any-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── Protected endpoints ───────────────────────────────────────────────────

    @Test
    void me_missingToken_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void me_validStudentToken_returns200WithProfile() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(STUDENT_EMAIL))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));
    }

    // ── Invalid / Expired / Tampered JWT ─────────────────────────────────────

    @Test
    void me_malformedJwt_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void me_tamperedJwt_returns401() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);
        String token = tokens.accessToken();
        String tampered = token.substring(0, token.length() - 10) + "TAMPERED!!";

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void me_garbageToken_returns401() throws Exception {
        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── RBAC ─────────────────────────────────────────────────────────────────

    @Test
    void rbac_studentAccessingAdminEndpoint_returns403ProblemDetail() throws Exception {
        TokenResponse tokens = register(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(get("/api/users/admin-only")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void rbac_officerAccessingAdminEndpoint_returns403() throws Exception {
        TokenResponse tokens = register(OFFICER_EMAIL, Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/users/admin-only")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void rbac_adminAccessingAdminEndpoint_returns200() throws Exception {
        TokenResponse tokens = register(ADMIN_EMAIL, Role.ROLE_ADMIN);

        mvc.perform(get("/api/users/admin-only")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TokenResponse register(String email, Role role) throws Exception {
        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(toJson(new RegisterRequest(email, TEST_PASSWORD, role))))
                .andExpect(status().isCreated())
                .andReturn();
        return fromJson(result, TokenResponse.class);
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private <T> T fromJson(MvcResult result, Class<T> type) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), type);
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
