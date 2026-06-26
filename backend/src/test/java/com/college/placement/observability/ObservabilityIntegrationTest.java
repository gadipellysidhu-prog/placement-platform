package com.college.placement.observability;

import com.college.placement.modules.auth.domain.AppUser;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = com.college.placement.Application.class,
    properties = {
        "management.endpoints.web.exposure.include=*",
        "management.prometheus.metrics.export.enabled=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

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

    @Test
    void actuatorHealth_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorMetrics_requiresAdminAuth() throws Exception {
        String adminToken = registerAdmin();

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheus_requiresAdminAuth() throws Exception {
        String adminToken = registerAdmin();

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    @Test
    void requestCorrelationFilter_addsTraceIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-ID"))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void requestCorrelationFilter_propagatesProvidedRequestId() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-ID", "my-custom-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "my-custom-id"));
    }

    @Test
    void prometheusEndpoint_containsAuthMetrics() throws Exception {
        String adminToken = registerAdmin();

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth_login_success_total")));
    }

    @Test
    void prometheusEndpoint_containsOutboxMetrics() throws Exception {
        String adminToken = registerAdmin();

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("outbox_pending_count")));
    }

    private String registerAdmin() throws Exception {
        AppUser admin = new AppUser();
        admin.setEmail("admin@obs.test");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(
                            new LoginRequest("admin@obs.test", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        TokenResponse tokens = objectMapper.readValue(
            result.getResponse().getContentAsString(), TokenResponse.class);
        return tokens.accessToken();
    }
}
