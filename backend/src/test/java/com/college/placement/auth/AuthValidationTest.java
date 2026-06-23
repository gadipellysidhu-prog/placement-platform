package com.college.placement.auth;

import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthValidationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String JSON         = MediaType.APPLICATION_JSON_VALUE;
    private static final String PROBLEM_JSON = "application/problem+json";

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    // ── Registration Validation ───────────────────────────────────────────────

    @Test
    void register_blankEmail_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void register_blankPassword_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("user@test.com", "", Role.ROLE_STUDENT))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void register_invalidEmailFormat_returns400() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("not-an-email", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409ProblemDetail() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("dup@test.com", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("dup@test.com", "password456", Role.ROLE_ADMIN))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void register_nullRole_returns400() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── Login Validation ──────────────────────────────────────────────────────

    @Test
    void login_blankEmail_returns400() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("", "password123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("user@test.com", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_nonExistentUser_returns401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("nobody@test.com", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("user@test.com", "correctPassword", Role.ROLE_STUDENT))))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("user@test.com", "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── Refresh Token Validation ──────────────────────────────────────────────

    @Test
    void refresh_invalidToken_returns401ProblemDetail() throws Exception {
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"totally-invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewTokens() throws Exception {
        MvcResult registerResult = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("refresh@test.com", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();

        TokenResponse tokens = mapper.readValue(
                registerResult.getResponse().getContentAsString(), TokenResponse.class);

        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_usedToken_returns401() throws Exception {
        MvcResult registerResult = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("reuse@test.com", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();

        TokenResponse tokens = mapper.readValue(
                registerResult.getResponse().getContentAsString(), TokenResponse.class);

        // Use the refresh token once
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk());

        // Reuse the same token — should fail (revoked)
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_validToken_returns200AndRevokesToken() throws Exception {
        MvcResult registerResult = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("logout@test.com", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();

        TokenResponse tokens = mapper.readValue(
                registerResult.getResponse().getContentAsString(), TokenResponse.class);

        mvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());

        // After logout, the refresh token should be invalid
        mvc.perform(post("/auth/refresh")
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_unknownToken_returns200Silently() throws Exception {
        MvcResult registerResult = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest("logoutanon@test.com", "password123", Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();
        TokenResponse tokens = mapper.readValue(
                registerResult.getResponse().getContentAsString(), TokenResponse.class);

        mvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(JSON)
                        .content("{\"refreshToken\":\"unknown-token-that-does-not-exist\"}"))
                .andExpect(status().isNoContent());
    }
}
