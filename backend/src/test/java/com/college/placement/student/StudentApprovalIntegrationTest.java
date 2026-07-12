package com.college.placement.student;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.student.dto.StudentApprovalRequest;
import com.college.placement.modules.student.repository.StudentRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the register → pending → approve → login workflow.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentApprovalIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String PASSWORD = "password123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        studentRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void fullApprovalWorkflow_pendingThenApprovedThenProfileVisible() throws Exception {
        String officer = tokenFor("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String studentEmail = "jane.doe@test.com";
        String studentToken = tokenFor(studentEmail, Role.ROLE_STUDENT);
        UUID userId = userRepo.findByEmail(studentEmail).orElseThrow().getId();

        // Before approval, the student has no profile → GET /me is 404.
        mvc.perform(get("/api/students/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());

        // The registration shows up as pending, with a derived display name.
        mvc.perform(get("/api/students/pending").header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email == '" + studentEmail + "')]").exists())
                .andExpect(jsonPath("$.content[0].displayName").value("Jane Doe"));

        // Approve → profile created and linked (201).
        StudentApprovalRequest req = new StudentApprovalRequest("CS2021010", null, 2);
        mvc.perform(post("/api/students/approvals/" + userId)
                        .header("Authorization", "Bearer " + officer)
                        .contentType(JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rollNumber").value("CS2021010"))
                .andExpect(jsonPath("$.userEmail").value(studentEmail))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // No longer pending.
        mvc.perform(get("/api/students/pending").header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email == '" + studentEmail + "')]").doesNotExist());

        // The student can now read their own profile.
        mvc.perform(get("/api/students/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value("CS2021010"));

        // And appears in the officer's students list (with skills serialised — guards the
        // lazy-initialization path that only triggers when the list has at least one row).
        mvc.perform(get("/api/students").header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.rollNumber == 'CS2021010')]").exists())
                .andExpect(jsonPath("$.content[0].skillNames").isArray());
    }

    @Test
    void approveTwice_returns409() throws Exception {
        String officer = tokenFor("officer2@test.com", Role.ROLE_PLACEMENT_OFFICER);
        UUID userId = user("dup@test.com", Role.ROLE_STUDENT).getId();

        StudentApprovalRequest req = new StudentApprovalRequest("CS2021011", null, 1);
        mvc.perform(post("/api/students/approvals/" + userId).header("Authorization", "Bearer " + officer)
                        .contentType(JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        StudentApprovalRequest again = new StudentApprovalRequest("CS2021012", null, 1);
        mvc.perform(post("/api/students/approvals/" + userId).header("Authorization", "Bearer " + officer)
                        .contentType(JSON).content(mapper.writeValueAsString(again)))
                .andExpect(status().isConflict());
    }

    @Test
    void approveUnknownUser_returns404() throws Exception {
        String officer = tokenFor("officer3@test.com", Role.ROLE_PLACEMENT_OFFICER);
        StudentApprovalRequest req = new StudentApprovalRequest("CS2021013", null, 1);
        mvc.perform(post("/api/students/approvals/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + officer)
                        .contentType(JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveNonStudentAccount_returns422() throws Exception {
        String officer = tokenFor("officer4@test.com", Role.ROLE_PLACEMENT_OFFICER);
        UUID adminId = user("another-admin@test.com", Role.ROLE_ADMIN).getId();

        StudentApprovalRequest req = new StudentApprovalRequest("CS2021014", null, 1);
        mvc.perform(post("/api/students/approvals/" + adminId).header("Authorization", "Bearer " + officer)
                        .contentType(JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pendingAndApprove_forbiddenForStudent() throws Exception {
        String student = tokenFor("plainstudent@test.com", Role.ROLE_STUDENT);
        UUID someUser = user("target@test.com", Role.ROLE_STUDENT).getId();

        mvc.perform(get("/api/students/pending").header("Authorization", "Bearer " + student))
                .andExpect(status().isForbidden());

        StudentApprovalRequest req = new StudentApprovalRequest("CS2021015", null, 1);
        mvc.perform(post("/api/students/approvals/" + someUser).header("Authorization", "Bearer " + student)
                        .contentType(JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AppUser user(String email, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmailVerified(true);
        return userRepo.save(u);
    }

    private String tokenFor(String email, Role role) throws Exception {
        user(email, role);
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }
}
