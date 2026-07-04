package com.college.placement.student;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.student.dto.StudentCreateRequest;
import com.college.placement.modules.student.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StudentRepository studentRepo;
    @Autowired com.college.placement.modules.placement.repository.OfferRepository offerRepo;
    @Autowired com.college.placement.modules.placement.repository.JobApplicationRepository applicationRepo;
    @Autowired com.college.placement.modules.certificate.repository.CertificateRepository certRepo;
    @Autowired com.college.placement.modules.company.repository.JobPostingRepository jobPostingRepo;
    @Autowired com.college.placement.modules.company.repository.CompanyRepository companyRepo;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @BeforeEach
    void clean() {
        offerRepo.deleteAll();
        applicationRepo.deleteAll();
        certRepo.deleteAll();
        studentRepo.deleteAll();
        jobPostingRepo.deleteAll();
        companyRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void createStudent_asOfficer_returns201() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String studentEmail = "student@test.com";
        TokenResponse studentTokens = register(studentEmail, Role.ROLE_STUDENT);
        UUID userId = userRepo.findByEmail(studentEmail).orElseThrow().getId();

        StudentCreateRequest req = new StudentCreateRequest(userId, "CS2021001", null, 2);

        mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rollNumber").value("CS2021001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createStudent_asStudent_returns403() throws Exception {
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        UUID userId = userRepo.findByEmail("student@test.com").orElseThrow().getId();

        StudentCreateRequest req = new StudentCreateRequest(userId, "CS2021002", null, 1);

        mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStudent_unauthorized_returns401() throws Exception {
        StudentCreateRequest req = new StudentCreateRequest(UUID.randomUUID(), "CS2021003", null, 1);

        mvc.perform(post("/api/students")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createStudent_validationFailure_returns400() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        // rollNumber is blank — validation should fail
        mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content("{\"userId\":\"" + UUID.randomUUID() + "\",\"rollNumber\":\"\",\"currentYear\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void listStudents_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getStudentMe_asStudent_returns200AfterProfileCreated() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        UUID userId = userRepo.findByEmail("student@test.com").orElseThrow().getId();

        mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new StudentCreateRequest(userId, "CS2021010", null, 1))));

        mvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value("CS2021010"));
    }

    @Test
    void getStudentById_notFound_returns404() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/students/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isNotFound());
    }

    private static final String TEST_PASSWORD = "password123";

    private TokenResponse register(String email, Role role) throws Exception {
        return createPrivilegedUser(email, role);
    }

    private TokenResponse createPrivilegedUser(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        user.setEmailVerified(true);
        userRepo.save(user);
        return login(email);
    }

    private TokenResponse login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }
}
