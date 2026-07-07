package com.college.placement.company;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.dto.CompanyCreateRequest;
import com.college.placement.modules.company.dto.RecruiterRegisterRequest;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.company.repository.RecruiterRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecruiterControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepo;
    @Autowired RecruiterRepository recruiterRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired JobApplicationRepository applicationRepo;
    @Autowired OfferRepository offerRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired AuditLogRepository auditLogRepo;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String TEST_PASSWORD = "password123";

    // Defensively clears the full dependency chain (shared H2 schema is reused across
    // test classes), mirroring CompanyControllerIntegrationTest ordering.
    @BeforeEach
    void clean() {
        recruiterRepo.deleteAll();
        offerRepo.deleteAll();
        applicationRepo.deleteAll();
        certRepo.deleteAll();
        studentRepo.deleteAll();
        jobPostingRepo.deleteAll();
        companyRepo.deleteAll();
        auditLogRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    // `recruiters` is populated only by this suite; clear it afterwards so the rows do
    // not leak into other test classes (which wipe companies/users but not recruiters).
    @AfterEach
    void tearDown() {
        recruiterRepo.deleteAll();
    }

    @Test
    void register_asOfficer_returns201_createsRow_publishesEventOnce_noEntityLeak() throws Exception {
        TokenResponse officer = login(privilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER));
        UUID companyId = createCompany(officer, "TechCorp");
        UUID targetUserId = plainUser("recruiter@test.com").getId();

        RecruiterRegisterRequest req = new RecruiterRegisterRequest(targetUserId, companyId, "Talent Lead");

        MvcResult result = mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()))
                .andExpect(jsonPath("$.userEmail").value("recruiter@test.com"))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.companyName").value("TechCorp"))
                .andExpect(jsonPath("$.designation").value("Talent Lead"))
                // No entity leakage: nested JPA entities / secrets must never be serialised.
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.company").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        UUID recruiterId = UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        // Recruiter row persisted exactly once.
        assertThat(recruiterRepo.count()).isEqualTo(1);
        assertThat(recruiterRepo.findById(recruiterId)).isPresent();

        // RecruiterRegisteredEvent published exactly once (one audit row via DomainEventAuditHandler).
        var logs = auditLogRepo.findByEntityTypeAndEntityId("Recruiter", recruiterId.toString(),
                PageRequest.of(0, 50)).getContent();
        assertThat(logs).filteredOn(l -> "RecruiterRegisteredEvent".equals(l.getAction())).hasSize(1);
    }

    @Test
    void register_asAdmin_returns201_viaRoleHierarchy() throws Exception {
        TokenResponse admin = login(privilegedUser("admin@test.com", Role.ROLE_ADMIN));
        UUID companyId = createCompany(admin, "AdminCorp");
        UUID targetUserId = plainUser("recruiter2@test.com").getId();

        RecruiterRegisterRequest req = new RecruiterRegisterRequest(targetUserId, companyId, "Recruiter");

        mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("AdminCorp"));
    }

    @Test
    void register_duplicateRecruiter_returns409() throws Exception {
        TokenResponse officer = login(privilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER));
        UUID companyId = createCompany(officer, "DupCorp");
        UUID targetUserId = plainUser("dup@test.com").getId();

        RecruiterRegisterRequest req = new RecruiterRegisterRequest(targetUserId, companyId, "First");
        mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration for the same user must conflict via the existing service check.
        mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RecruiterRegisterRequest(targetUserId, companyId, "Second"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(recruiterRepo.count()).isEqualTo(1);
    }

    @Test
    void register_companyNotFound_returns404() throws Exception {
        TokenResponse officer = login(privilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER));
        UUID targetUserId = plainUser("nocompany@test.com").getId();

        RecruiterRegisterRequest req = new RecruiterRegisterRequest(targetUserId, UUID.randomUUID(), "Recruiter");

        mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        assertThat(recruiterRepo.count()).isZero();
    }

    @Test
    void register_userNotFound_returns404() throws Exception {
        TokenResponse officer = login(privilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER));
        UUID companyId = createCompany(officer, "NoUserCorp");

        RecruiterRegisterRequest req = new RecruiterRegisterRequest(UUID.randomUUID(), companyId, "Recruiter");

        mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        assertThat(recruiterRepo.count()).isZero();
    }

    @Test
    void register_asStudent_returns403() throws Exception {
        TokenResponse student = login(privilegedUser("student@test.com", Role.ROLE_STUDENT));
        UUID targetUserId = plainUser("recruiter3@test.com").getId();

        RecruiterRegisterRequest req = new RecruiterRegisterRequest(targetUserId, UUID.randomUUID(), "Recruiter");

        mvc.perform(post("/api/recruiters")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_unauthorized_returns401() throws Exception {
        mvc.perform(post("/api/recruiters")
                        .contentType(JSON)
                        .content("{\"userId\":\"" + UUID.randomUUID() + "\",\"companyId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Helpers

    private AppUser privilegedUser(String email, Role role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        user.setEmailVerified(true);
        return userRepo.save(user);
    }

    private AppUser plainUser(String email) {
        return privilegedUser(email, Role.ROLE_STUDENT);
    }

    private TokenResponse login(AppUser user) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(user.getEmail(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private UUID createCompany(TokenResponse token, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + token.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new CompanyCreateRequest(name, null, "Technology", null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
