package com.college.placement.dashboard;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.dto.CompanyCreateRequest;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.repository.BranchRepository;
import com.college.placement.modules.student.repository.SkillRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired OfferRepository offerRepo;
    @Autowired JobApplicationRepository applicationRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired BranchRepository branchRepo;
    @Autowired SkillRepository skillRepo;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void clean() {
        offerRepo.deleteAll();
        applicationRepo.deleteAll();
        certRepo.deleteAll();
        studentRepo.deleteAll();
        jobPostingRepo.deleteAll();
        companyRepo.deleteAll();
        branchRepo.deleteAll();
        skillRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void summary_asOfficer_returns200WithCounts() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        // Create a company so activeCompanies > 0
        mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new CompanyCreateRequest("TestCorp", null, "Technology", null))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").isNumber())
                .andExpect(jsonPath("$.placedStudents").isNumber())
                .andExpect(jsonPath("$.activeCompanies").value(1))
                .andExpect(jsonPath("$.openJobPostings").isNumber())
                .andExpect(jsonPath("$.pendingApplications").isNumber())
                .andExpect(jsonPath("$.pendingCertificates").isNumber())
                .andExpect(jsonPath("$.placementRatePercent").isNumber());
    }

    @Test
    void summary_asStudent_returns403() throws Exception {
        TokenResponse student = registerStudent("student@test.com");

        mvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_empty_returns200WithZeroCounts() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(0))
                .andExpect(jsonPath("$.placedStudents").value(0))
                .andExpect(jsonPath("$.activeCompanies").value(0))
                .andExpect(jsonPath("$.openJobPostings").value(0))
                .andExpect(jsonPath("$.pendingApplications").value(0))
                .andExpect(jsonPath("$.pendingCertificates").value(0))
                .andExpect(jsonPath("$.placementRatePercent").value(0.0));
    }

    // Helpers

    private TokenResponse registerStudent(String email) throws Exception {
        return createPrivilegedUser(email, Role.ROLE_STUDENT);
    }

    private TokenResponse createPrivilegedUser(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        user.setEmailVerified(true);
        userRepo.save(user);
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }
}
