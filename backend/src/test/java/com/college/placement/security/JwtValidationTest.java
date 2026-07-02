package com.college.placement.security;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies JWT validation edge-cases:
 *  - Expired access token
 *  - Token signed with a different RSA key pair (forged)
 *  - Token with wrong issuer (valid signature, wrong iss claim)
 *  - Token with wrong audience (valid signature, wrong aud claim)
 *  - Token with empty subject
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtValidationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String JSON         = MediaType.APPLICATION_JSON_VALUE;
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String EMAIL        = "jwt-test@example.com";
    private static final String PASSWORD     = "password123";

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            refreshTokenRepo.deleteAll();
            userRepo.deleteAll();
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    // ── Expired token ─────────────────────────────────────────────────────────

    @Test
    void expiredAccessToken_returns401() throws Exception {
        register();
        String expired = signedToken(EMAIL, "ROLE_STUDENT",
                "placement-platform", "placement-api",
                new Date(System.currentTimeMillis() - 120_000)); // 2 min ago

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── Token signed with a different RSA key (forged) ────────────────────────

    @Test
    void tokenSignedWithDifferentKey_returns401() throws Exception {
        register();
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        RSAPrivateKey attackerKey = (RSAPrivateKey) gen.generateKeyPair().getPrivate();

        String forged = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(EMAIL)
                .claim("role", "ROLE_STUDENT")
                .issuer("placement-platform")
                .audience().add("placement-api").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(attackerKey)
                .compact();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── Wrong issuer (signature valid) ────────────────────────────────────────

    @Test
    void tokenWithWrongIssuer_returns401() throws Exception {
        register();
        // Sign with real private key so signature passes; issuer mismatch must then reject it.
        String wrongIssuer = signedToken(EMAIL, "ROLE_STUDENT",
                "evil-issuer", "placement-api",
                new Date(System.currentTimeMillis() + 900_000));

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + wrongIssuer))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void validateAndParse_wrongIssuer_returnsEmpty() {
        String bad = signedToken(EMAIL, "ROLE_STUDENT", "wrong-issuer", "placement-api",
                new Date(System.currentTimeMillis() + 900_000));
        assertThat(jwtService.validateAndParse(bad)).isEmpty();
    }

    // ── Wrong audience (signature valid) ──────────────────────────────────────

    @Test
    void tokenWithWrongAudience_returns401() throws Exception {
        register();
        String wrongAud = signedToken(EMAIL, "ROLE_STUDENT",
                "placement-platform", "wrong-audience",
                new Date(System.currentTimeMillis() + 900_000));

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + wrongAud))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void validateAndParse_wrongAudience_returnsEmpty() {
        String bad = signedToken(EMAIL, "ROLE_STUDENT", "placement-platform", "wrong-audience",
                new Date(System.currentTimeMillis() + 900_000));
        assertThat(jwtService.validateAndParse(bad)).isEmpty();
    }

    // ── JwtService unit-level validation ─────────────────────────────────────

    @Test
    void validateAndParse_validToken_returnsPresent() {
        String token = jwtService.generateAccessToken(EMAIL, "ROLE_STUDENT");
        assertThat(jwtService.validateAndParse(token)).isPresent();
    }

    @Test
    void validateAndParse_expiredToken_returnsEmpty() {
        String expired = signedToken(EMAIL, "ROLE_STUDENT",
                "placement-platform", "placement-api",
                new Date(System.currentTimeMillis() - 60_000));
        assertThat(jwtService.validateAndParse(expired)).isEmpty();
    }

    @Test
    void validateAndParse_malformedString_returnsEmpty() {
        assertThat(jwtService.validateAndParse("not.a.jwt")).isEmpty();
    }

    @Test
    void validateAndParse_emptyString_returnsEmpty() {
        assertThat(jwtService.validateAndParse("")).isEmpty();
    }

    @Test
    void validateAndParse_randomBytes_returnsEmpty() {
        assertThat(jwtService.validateAndParse("aGVsbG8=.d29ybGQ=.dGVzdA==")).isEmpty();
    }

    // ── Valid token still accepted ────────────────────────────────────────────

    @Test
    void validToken_returns200() throws Exception {
        TokenResponse t = register();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + t.accessToken()))
                .andExpect(status().isOk());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TokenResponse register() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated());
        AppUser user = userRepo.findByEmail(EMAIL).orElseThrow();
        user.setEmailVerified(true);
        userRepo.save(user);
        MvcResult r = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readValue(r.getResponse().getContentAsString(), TokenResponse.class);
    }

    /**
     * Builds a token signed with the application's own ephemeral RSA private key
     * but with caller-supplied issuer, audience, and expiry.
     * This lets us test issuer/audience validation independent of signature failure.
     */
    private String signedToken(String subject, String role,
                               String issuer, String audience, Date expiry) {
        RSAPrivateKey pk = (RSAPrivateKey) ReflectionTestUtils.getField(jwtService, "privateKey");
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expiry)
                .signWith(pk);
        if (subject  != null) builder.subject(subject);
        if (issuer   != null) builder.issuer(issuer);
        if (audience != null) builder.audience().add(audience).and();
        return builder.compact();
    }
}
