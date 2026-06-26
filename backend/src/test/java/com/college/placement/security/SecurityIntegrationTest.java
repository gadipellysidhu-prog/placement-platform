package com.college.placement.security;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.shared.ratelimit.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = com.college.placement.Application.class,
    properties = {"management.prometheus.metrics.export.enabled=true"}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RateLimitFilter rateLimitFilter;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String JSON         = MediaType.APPLICATION_JSON_VALUE;
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TEST_EMAIL   = "security@test.com";
    private static final String ADMIN_EMAIL  = "admin@test.com";
    private static final String TEST_PASS    = "password123";

    @BeforeEach
    void cleanDb() {
        // Disable FK checks so we can cleanly wipe auth tables regardless of child rows
        // created by other integration tests sharing the same H2 context.
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
        rateLimitFilter.clearAll();
    }

    // ── Security Headers ──────────────────────────────────────────────────────

    @Test
    void securityHeaders_onPublicEndpoint_returnsRequiredHeaders() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("Permissions-Policy"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void securityHeaders_xContentTypeOptions_isNoSniff() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void securityHeaders_xFrameOptions_isDeny() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void securityHeaders_referrerPolicy_isStrictOriginWhenCrossOrigin() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void securityHeaders_csp_containsDefaultSrc() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    // ── JWT Authentication Tests ──────────────────────────────────────────────

    @Test
    void jwt_missingToken_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void jwt_malformedToken_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void jwt_tamperedToken_returns401() throws Exception {
        TokenResponse tokens = register(TEST_EMAIL, Role.ROLE_STUDENT);
        String token    = tokens.accessToken();
        String tampered = token.substring(0, token.length() - 10) + "TAMPERED!!";

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void jwt_validToken_returns200() throws Exception {
        TokenResponse tokens = register(TEST_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    // ── Brute-Force / Account Lockout Tests ──────────────────────────────────

    @Test
    void bruteForce_singleFailedLogin_returns401NotLocked() throws Exception {
        register(TEST_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(TEST_EMAIL, "wrongpassword"))))
                .andExpect(status().isUnauthorized());

        AppUser user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertThat(user.getFailureCount()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void bruteForce_fiveConsecutiveFailures_locksAccount() throws Exception {
        register(TEST_EMAIL, Role.ROLE_STUDENT);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login")
                            .contentType(JSON)
                            .content(toJson(new LoginRequest(TEST_EMAIL, "wrongpassword"))))
                    .andExpect(status().isUnauthorized());
        }

        AppUser user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertThat(user.getFailureCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void bruteForce_lockedAccount_returns423() throws Exception {
        register(TEST_EMAIL, Role.ROLE_STUDENT);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login")
                            .contentType(JSON)
                            .content(toJson(new LoginRequest(TEST_EMAIL, "wrongpassword"))));
        }

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(TEST_EMAIL, TEST_PASS))))
                .andExpect(status().isLocked())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void bruteForce_successfulLogin_resetsFailureCount() throws Exception {
        register(TEST_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(TEST_EMAIL, "wrongpassword"))));

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(TEST_EMAIL, TEST_PASS))))
                .andExpect(status().isOk());

        AppUser user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertThat(user.getFailureCount()).isEqualTo(0);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void bruteForce_expiredLock_allowsLogin() throws Exception {
        register(TEST_EMAIL, Role.ROLE_STUDENT);

        AppUser user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        user.setFailureCount(5);
        user.setLockedUntil(Instant.now().minusSeconds(1));
        userRepository.save(user);

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(TEST_EMAIL, TEST_PASS))))
                .andExpect(status().isOk());
    }

    // ── Rate Limiting Tests ───────────────────────────────────────────────────

    @Test
    void rateLimit_disabled_inTestProfile_requestsSucceed() throws Exception {
        register(TEST_EMAIL, Role.ROLE_STUDENT);

        // Make more than the login limit (5) to verify rate limiting is disabled in tests
        for (int i = 0; i < 7; i++) {
            mvc.perform(post("/auth/login")
                            .contentType(JSON)
                            .content(toJson(new LoginRequest(TEST_EMAIL, TEST_PASS))))
                    .andExpect(status().isOk());
        }
    }

    // ── Actuator Authorization Tests ──────────────────────────────────────────

    @Test
    void actuator_health_isPublicWithoutAuth() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuator_metrics_requiresAuth_returns401WhenAnonymous() throws Exception {
        mvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void actuator_info_requiresAuth_returns401WhenAnonymous() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void actuator_prometheus_requiresAuth_returns401WhenAnonymous() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void actuator_metrics_returns403WhenStudentAuthenticated() throws Exception {
        TokenResponse tokens = register(TEST_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void actuator_metrics_returns403WhenPlacementOfficerAuthenticated() throws Exception {
        // Placement Officer is below ADMIN in the hierarchy; actuator requires ROLE_ADMIN explicitly
        TokenResponse tokens = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void actuator_prometheus_returns403WhenPlacementOfficerAuthenticated() throws Exception {
        TokenResponse tokens = register("officer2@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void forbiddenEndpoint_placementOfficerAccessingAdminEndpoint_returns403() throws Exception {
        // Placement Officer inherits Student permissions but not Admin-only endpoints
        TokenResponse tokens = register("officer3@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/users/admin-only")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void actuator_metrics_returns200WhenAdminAuthenticated() throws Exception {
        TokenResponse tokens = register(ADMIN_EMAIL, Role.ROLE_ADMIN);

        mvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void actuator_prometheus_returns200WhenAdminAuthenticated() throws Exception {
        TokenResponse tokens = register(ADMIN_EMAIL, Role.ROLE_ADMIN);

        mvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    // ── CORS Tests ────────────────────────────────────────────────────────────

    @Test
    void cors_allowedOrigin_returns200WithCorsHeader() throws Exception {
        mvc.perform(get("/api/users/me")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void cors_disallowedOrigin_noCorsHeader() throws Exception {
        mvc.perform(get("/api/users/me")
                        .header("Origin", "http://evil.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void cors_preflight_options_returnsOk() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/users/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    // ── Forbidden Endpoint Tests ──────────────────────────────────────────────

    @Test
    void forbiddenEndpoint_studentAccessingAdminEndpoint_returns403() throws Exception {
        TokenResponse tokens = register(TEST_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(get("/api/users/admin-only")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void forbiddenEndpoint_adminAccessingAdminEndpoint_returns200() throws Exception {
        TokenResponse tokens = register(ADMIN_EMAIL, Role.ROLE_ADMIN);

        mvc.perform(get("/api/users/admin-only")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void swagger_isPublicWithoutAuth() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TokenResponse register(String email, Role role) throws Exception {
        if (role == Role.ROLE_STUDENT) {
            MvcResult result = mvc.perform(post("/auth/register")
                            .contentType(JSON)
                            .content(toJson(new RegisterRequest(email, TEST_PASS, role))))
                    .andExpect(status().isCreated())
                    .andReturn();
            return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
        }
        return createPrivilegedUser(email, role);
    }

    private TokenResponse createPrivilegedUser(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASS));
        user.setRole(role);
        userRepository.save(user);
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(toJson(new LoginRequest(email, TEST_PASS))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
