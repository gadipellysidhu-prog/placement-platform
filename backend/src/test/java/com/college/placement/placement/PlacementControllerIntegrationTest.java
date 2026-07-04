package com.college.placement.placement;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.dto.CompanyCreateRequest;
import com.college.placement.modules.company.dto.JobPostingCreateRequest;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.dto.JobApplicationCreateRequest;
import com.college.placement.modules.placement.dto.JobApplicationStatusUpdateRequest;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.dto.StudentCreateRequest;
import com.college.placement.modules.student.dto.StudentUpdateRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlacementControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StudentRepository studentRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired OfferRepository offerRepo;
    @Autowired JobApplicationRepository applicationRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired CompanyRepository companyRepo;

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
    void apply_eligibleStudent_returns201() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        UUID studentId = createStudentAndMakeEligible(officer, "student@test.com", "CS2021001");
        String postingId = createOpenPosting(officer);

        JobApplicationCreateRequest req = new JobApplicationCreateRequest(studentId, UUID.fromString(postingId));

        mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void apply_unauthorized_returns401() throws Exception {
        mvc.perform(post("/api/applications")
                        .contentType(JSON)
                        .content("{\"studentId\":\"" + UUID.randomUUID() + "\",\"jobPostingId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listApplications_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listApplications_asStudent_returns403() throws Exception {
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        UUID studentId = createStudentAndMakeEligible(officer, "student@test.com", "CS2021002");
        String postingId = createOpenPosting(officer);

        String appId = applyToPosting(student, studentId, UUID.fromString(postingId));

        mvc.perform(put("/api/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new JobApplicationStatusUpdateRequest(ApplicationStatus.SHORTLISTED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    @Test
    void withdraw_asStudent_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        UUID studentId = createStudentAndMakeEligible(officer, "student@test.com", "CS2021003");
        String postingId = createOpenPosting(officer);

        String appId = applyToPosting(student, studentId, UUID.fromString(postingId));

        mvc.perform(post("/api/applications/" + appId + "/withdraw")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    void getApplicationById_notFound_returns404() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/applications/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isNotFound());
    }

    // Helpers

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

    private UUID createStudentAndMakeEligible(TokenResponse officer, String studentEmail, String rollNumber) throws Exception {
        UUID userId = userRepo.findByEmail(studentEmail).orElseThrow().getId();
        MvcResult result = mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new StudentCreateRequest(userId, rollNumber, null, 2))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID studentId = UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        // Set CGPA >= 5.0 so student is eligible
        mvc.perform(put("/api/students/" + studentId)
                .header("Authorization", "Bearer " + officer.accessToken())
                .contentType(JSON)
                .content(mapper.writeValueAsString(new StudentUpdateRequest(null, new BigDecimal("7.5"), 2))));

        // Evaluate eligibility
        mvc.perform(put("/api/students/" + studentId + "/eligibility")
                .header("Authorization", "Bearer " + officer.accessToken()));

        return studentId;
    }

    private String createOpenPosting(TokenResponse officer) throws Exception {
        String companyId = createCompany(officer, "TestCorp");
        JobPostingCreateRequest req = new JobPostingCreateRequest(
                UUID.fromString(companyId), "Software Engineer", "Java dev", new BigDecimal("8"), new BigDecimal("15"), null, 5);
        MvcResult result = mvc.perform(post("/api/job-postings")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String postingId = mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/job-postings/" + postingId + "/open")
                .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());
        return postingId;
    }

    private String createCompany(TokenResponse officer, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new CompanyCreateRequest(name, null, "Technology", null))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String applyToPosting(TokenResponse student, UUID studentId, UUID postingId) throws Exception {
        MvcResult result = mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new JobApplicationCreateRequest(studentId, postingId))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
