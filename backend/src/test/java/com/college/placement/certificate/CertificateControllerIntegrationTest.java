package com.college.placement.certificate;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.dto.CertificateCreateRequest;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.student.dto.StudentCreateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CertificateControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StudentRepository studentRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired com.college.placement.modules.placement.repository.OfferRepository offerRepo;
    @Autowired com.college.placement.modules.placement.repository.JobApplicationRepository applicationRepo;
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
    void submitCertificate_asStudent_returns201() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        UUID studentId = createStudentProfile(officer, "student@test.com", "CS2021001");

        CertificateCreateRequest req = new CertificateCreateRequest(
                studentId, "AWS Certified", "Amazon", null, "file/cert.pdf");

        mvc.perform(post("/api/certificates")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("AWS Certified"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    void submitCertificate_unauthorized_returns401() throws Exception {
        CertificateCreateRequest req = new CertificateCreateRequest(
                UUID.randomUUID(), "Test Cert", "Org", null, null);

        mvc.perform(post("/api/certificates")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCertificates_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/certificates")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listCertificates_asStudent_returns403() throws Exception {
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        mvc.perform(get("/api/certificates")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyCertificate_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        UUID studentId = createStudentProfile(officer, "student@test.com", "CS2021002");

        String certId = createCertificate(student, studentId, "Oracle Certified");

        mvc.perform(post("/api/certificates/" + certId + "/verify")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }

    @Test
    void rejectCertificate_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        UUID studentId = createStudentProfile(officer, "student@test.com", "CS2021003");

        String certId = createCertificate(student, studentId, "Some Cert");

        mvc.perform(post("/api/certificates/" + certId + "/reject")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"));
    }

    @Test
    void submitCertificate_missingName_returns400() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        UUID studentId = createStudentProfile(officer, "student@test.com", "CS2021004");

        mvc.perform(post("/api/certificates")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content("{\"studentId\":\"" + studentId + "\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
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

    private UUID createStudentProfile(TokenResponse officer, String studentEmail, String rollNumber) throws Exception {
        UUID userId = userRepo.findByEmail(studentEmail).orElseThrow().getId();
        MvcResult result = mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new StudentCreateRequest(userId, rollNumber, null, 1))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String createCertificate(TokenResponse student, UUID studentId, String name) throws Exception {
        CertificateCreateRequest req = new CertificateCreateRequest(studentId, name, "Org", null, "file.pdf");
        MvcResult result = mvc.perform(post("/api/certificates")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
