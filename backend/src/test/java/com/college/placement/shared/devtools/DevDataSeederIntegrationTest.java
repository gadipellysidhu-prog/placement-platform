package com.college.placement.shared.devtools;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@link DevDataSeeder} produces demo accounts that log in through the
 * real authentication endpoint and pass role-based authorization.
 *
 * <p>The seeder bean is {@code @Profile("dev")}; this test runs under {@code test},
 * so it invokes the seeder directly (the profile gate only governs Spring bean
 * registration, not the seeding logic). Everything after — password hashing,
 * {@code POST /auth/login}, JWT issuance, and the {@code /api/admin/users} guard —
 * exercises the production code paths unchanged.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevDataSeederIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@placement.local";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String STUDENT_EMAIL = "student@placement.local";
    private static final String STUDENT_PASSWORD = "Student@123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void runSeeder() {
        new DevDataSeeder(userRepository, passwordEncoder).run(null);
    }

    @Test
    void seedsBothAccountsWithLoginReadyFieldsAndIsIdempotent() {
        runSeeder();
        runSeeder(); // second run must not create duplicates

        assertThat(userRepository.count()).isEqualTo(2);

        AppUser admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(admin.isEmailVerified()).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();

        AppUser student = userRepository.findByEmail(STUDENT_EMAIL).orElseThrow();
        assertThat(student.getRole()).isEqualTo(Role.ROLE_STUDENT);
        assertThat(student.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(student.isEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches(STUDENT_PASSWORD, student.getPasswordHash())).isTrue();
    }

    @Test
    void adminAndStudentLogInAndRoleAuthorizationIsEnforced() throws Exception {
        runSeeder();

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String studentToken = login(STUDENT_EMAIL, STUDENT_PASSWORD);
        assertThat(adminToken).isNotBlank();
        assertThat(studentToken).isNotBlank();

        // Admin-only endpoint: admin allowed, student forbidden, anonymous unauthorized.
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
