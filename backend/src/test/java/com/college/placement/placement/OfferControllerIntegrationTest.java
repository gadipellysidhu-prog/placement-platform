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
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.dto.JobApplicationCreateRequest;
import com.college.placement.modules.placement.dto.JobApplicationStatusUpdateRequest;
import com.college.placement.modules.placement.dto.OfferCreateRequest;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OfferControllerIntegrationTest {

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
    void createOffer_asOfficer_returns201() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        String appId = setupApplicationInOfferedState(officer, student, "CS2021001");

        OfferCreateRequest req = new OfferCreateRequest(UUID.fromString(appId), new BigDecimal("12.5"), LocalDate.now().plusMonths(6));

        mvc.perform(post("/api/offers")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.ctc").value(12.5));
    }

    @Test
    void createOffer_unauthorized_returns401() throws Exception {
        mvc.perform(post("/api/offers")
                        .contentType(JSON)
                        .content("{\"applicationId\":\"" + UUID.randomUUID() + "\",\"ctc\":10.0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOffers_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/offers")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listOffers_asStudent_returns403() throws Exception {
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        mvc.perform(get("/api/offers")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptOffer_asStudent_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        String appId = setupApplicationInOfferedState(officer, student, "CS2021002");
        String offerId = createOffer(officer, UUID.fromString(appId));

        mvc.perform(post("/api/offers/" + offerId + "/accept")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void rejectOffer_asStudent_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        String appId = setupApplicationInOfferedState(officer, student, "CS2021003");
        String offerId = createOffer(officer, UUID.fromString(appId));

        mvc.perform(post("/api/offers/" + offerId + "/reject")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void getOfferById_notFound_returns404() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/offers/" + UUID.randomUUID())
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

    private String setupApplicationInOfferedState(TokenResponse officer, TokenResponse student, String rollNumber) throws Exception {
        // Create student profile
        UUID userId = userRepo.findByEmail("student@test.com").orElseThrow().getId();
        MvcResult sResult = mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new StudentCreateRequest(userId, rollNumber, null, 2))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID studentId = UUID.fromString(mapper.readTree(sResult.getResponse().getContentAsString()).get("id").asText());

        // Set eligible
        mvc.perform(put("/api/students/" + studentId)
                .header("Authorization", "Bearer " + officer.accessToken())
                .contentType(JSON)
                .content(mapper.writeValueAsString(new StudentUpdateRequest(null, new BigDecimal("8.0"), 2))));
        mvc.perform(put("/api/students/" + studentId + "/eligibility")
                .header("Authorization", "Bearer " + officer.accessToken()));

        // Create company and job posting
        String companyId = createCompany(officer);
        String postingId = createOpenPosting(officer, UUID.fromString(companyId));

        // Apply
        String appId = applyToPosting(student, studentId, UUID.fromString(postingId));

        // Move to OFFERED via SHORTLISTED → INTERVIEWED → OFFERED
        updateAppStatus(officer, appId, ApplicationStatus.SHORTLISTED);
        updateAppStatus(officer, appId, ApplicationStatus.INTERVIEWED);
        updateAppStatus(officer, appId, ApplicationStatus.OFFERED);

        return appId;
    }

    private String createCompany(TokenResponse officer) throws Exception {
        MvcResult result = mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new CompanyCreateRequest("OfferCorp", null, "Tech", null))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createOpenPosting(TokenResponse officer, UUID companyId) throws Exception {
        JobPostingCreateRequest req = new JobPostingCreateRequest(
                companyId, "Dev Role", "Desc", new BigDecimal("8"), new BigDecimal("15"), null, 3);
        MvcResult result = mvc.perform(post("/api/job-postings")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/job-postings/" + id + "/open")
                .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());
        return id;
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

    private void updateAppStatus(TokenResponse officer, String appId, ApplicationStatus status) throws Exception {
        mvc.perform(put("/api/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new JobApplicationStatusUpdateRequest(status))))
                .andExpect(status().isOk());
    }

    private String createOffer(TokenResponse officer, UUID appId) throws Exception {
        OfferCreateRequest req = new OfferCreateRequest(appId, new BigDecimal("12.0"), LocalDate.now().plusMonths(6));
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
