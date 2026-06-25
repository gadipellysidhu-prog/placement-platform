package com.college.placement.security;

import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.dto.CertificateCreateRequest;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.dto.CompanyCreateRequest;
import com.college.placement.modules.company.dto.JobPostingCreateRequest;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.dto.JobApplicationCreateRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that students cannot act on behalf of other students.
 * Each test proves a specific privilege-escalation vector is blocked.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationOwnershipTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
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

    // ── Apply: student cannot apply using another student's ID ────────────────

    @Test
    void apply_studentUsesAnotherStudentId_returns403() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student1 = register("student1@test.com", Role.ROLE_STUDENT);
        register("student2@test.com", Role.ROLE_STUDENT);

        UUID student1Id = createEligibleStudent(officer, "student1@test.com", "ROLL001");
        UUID student2Id = createEligibleStudent(officer, "student2@test.com", "ROLL002");
        String postingId = createOpenPosting(officer);

        // student1 tries to apply using student2's ID → 403
        mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + student1.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new JobApplicationCreateRequest(student2Id, UUID.fromString(postingId)))))
                .andExpect(status().isForbidden());
    }

    // ── Withdraw: student cannot withdraw another student's application ────────

    @Test
    void withdraw_studentWithdrawsOtherStudentApplication_returns403() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student1 = register("student1@test.com", Role.ROLE_STUDENT);
        TokenResponse student2 = register("student2@test.com", Role.ROLE_STUDENT);

        UUID student1Id = createEligibleStudent(officer, "student1@test.com", "ROLL003");
        UUID student2Id = createEligibleStudent(officer, "student2@test.com", "ROLL004");
        String postingId = createOpenPosting(officer);

        // student1 applies legitimately
        String appId = applyToPosting(student1, student1Id, UUID.fromString(postingId));

        // student2 tries to withdraw student1's application → 403
        mvc.perform(post("/api/applications/" + appId + "/withdraw")
                        .header("Authorization", "Bearer " + student2.accessToken()))
                .andExpect(status().isForbidden());
    }

    // ── Certificate: student cannot submit for another student ────────────────

    @Test
    void submitCertificate_studentUsesAnotherStudentId_returns403() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student1 = register("student1@test.com", Role.ROLE_STUDENT);
        register("student2@test.com", Role.ROLE_STUDENT);

        createEligibleStudent(officer, "student1@test.com", "ROLL005");
        UUID student2Id = createEligibleStudent(officer, "student2@test.com", "ROLL006");

        // student1 tries to submit a certificate for student2 → 403
        var req = new CertificateCreateRequest(student2Id, "AWS Cert", "Amazon", null, null);
        mvc.perform(post("/api/certificates")
                        .header("Authorization", "Bearer " + student1.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Own resources: student CAN use own ID ────────────────────────────────

    @Test
    void apply_studentUsesOwnId_returns201() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        UUID studentId = createEligibleStudent(officer, "student@test.com", "ROLL007");
        String postingId = createOpenPosting(officer);

        mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new JobApplicationCreateRequest(studentId, UUID.fromString(postingId)))))
                .andExpect(status().isCreated());
    }

    @Test
    void withdraw_studentWithdrawsOwnApplication_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);

        UUID studentId = createEligibleStudent(officer, "student@test.com", "ROLL008");
        String postingId = createOpenPosting(officer);
        String appId = applyToPosting(student, studentId, UUID.fromString(postingId));

        mvc.perform(post("/api/applications/" + appId + "/withdraw")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TokenResponse register(String email, Role role) throws Exception {
        MvcResult r = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, "password123", role))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readValue(r.getResponse().getContentAsString(), TokenResponse.class);
    }

    private UUID createEligibleStudent(TokenResponse officer, String email, String roll) throws Exception {
        UUID userId = userRepo.findByEmail(email).orElseThrow().getId();
        MvcResult r = mvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new StudentCreateRequest(userId, roll, null, 2))))
                .andExpect(status().isCreated()).andReturn();
        UUID sid = UUID.fromString(mapper.readTree(r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(put("/api/students/" + sid)
                .header("Authorization", "Bearer " + officer.accessToken())
                .contentType(JSON)
                .content(mapper.writeValueAsString(new StudentUpdateRequest(null, new BigDecimal("7.5"), 2))));
        mvc.perform(put("/api/students/" + sid + "/eligibility")
                .header("Authorization", "Bearer " + officer.accessToken()));
        return sid;
    }

    private String createOpenPosting(TokenResponse officer) throws Exception {
        MvcResult cr = mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new CompanyCreateRequest("TestCorp-" + UUID.randomUUID(), null, "Technology", null))))
                .andExpect(status().isCreated()).andReturn();
        String companyId = mapper.readTree(cr.getResponse().getContentAsString()).get("id").asText();

        MvcResult pr = mvc.perform(post("/api/job-postings")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new JobPostingCreateRequest(
                                UUID.fromString(companyId), "SWE", "Java", new BigDecimal("8"), new BigDecimal("15"), null, 5))))
                .andExpect(status().isCreated()).andReturn();
        String postingId = mapper.readTree(pr.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/job-postings/" + postingId + "/open")
                .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());
        return postingId;
    }

    private String applyToPosting(TokenResponse student, UUID studentId, UUID postingId) throws Exception {
        MvcResult r = mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new JobApplicationCreateRequest(studentId, postingId))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }
}
