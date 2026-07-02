package com.college.placement.company;

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
import com.college.placement.modules.company.dto.JobPostingUpdateRequest;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.notification.repository.NotificationHistoryRepository;
import com.college.placement.modules.placement.repository.ApplicationStatusHistoryRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.dto.BranchCreateRequest;
import com.college.placement.modules.student.dto.SkillCreateRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobPostingControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StudentRepository studentRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired OfferRepository offerRepo;
    @Autowired ApplicationStatusHistoryRepository statusHistoryRepo;
    @Autowired NotificationHistoryRepository notificationHistoryRepo;
    @Autowired JobApplicationRepository applicationRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired SkillRepository skillRepo;
    @Autowired BranchRepository branchRepo;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @BeforeEach
    void clean() {
        offerRepo.deleteAll();
        statusHistoryRepo.deleteAll();
        applicationRepo.deleteAll();
        certRepo.deleteAll();
        studentRepo.deleteAll();
        jobPostingRepo.deleteAll();
        companyRepo.deleteAll();
        skillRepo.deleteAll();
        branchRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        notificationHistoryRepo.deleteAll();
        userRepo.deleteAll();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void create_asOfficer_returns201() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "Acme Corp");

        JobPostingCreateRequest req = new JobPostingCreateRequest(
                UUID.fromString(companyId), "Backend Engineer", "Write Java code",
                new BigDecimal("8"), new BigDecimal("15"), null, 3);

        mvc.perform(post("/api/job-postings")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.offerLimit").value(3));
    }

    @Test
    void create_asStudent_returns403() throws Exception {
        TokenResponse student = registerStudent("student@test.com");

        JobPostingCreateRequest req = new JobPostingCreateRequest(
                UUID.randomUUID(), "Title", null, null, null, null, 1);

        mvc.perform(post("/api/job-postings")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mvc.perform(post("/api/job-postings")
                        .contentType(JSON)
                        .content("{\"companyId\":\"" + UUID.randomUUID() + "\",\"title\":\"T\",\"offerLimit\":1}"))
                .andExpect(status().isUnauthorized());
    }

    // ── List open (student) ───────────────────────────────────────────────────

    @Test
    void listOpen_asStudent_returns200WithOnlyOpenPostings() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        TokenResponse student = registerStudent("student@test.com");
        String companyId = createCompany(officer, "TechCorp");

        // Create one DRAFT and one OPEN posting
        String draftId = createPosting(officer, companyId, "Draft Position");
        String openId = createPosting(officer, companyId, "Open Position");
        openPosting(officer, openId);

        mvc.perform(get("/api/job-postings")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    void listOpen_withTitleFilter_returnsFiltered() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        TokenResponse student = registerStudent("student@test.com");
        String companyId = createCompany(officer, "TechCorp2");

        String id1 = createPosting(officer, companyId, "Java Developer");
        String id2 = createPosting(officer, companyId, "Python Engineer");
        openPosting(officer, id1);
        openPosting(officer, id2);

        mvc.perform(get("/api/job-postings?title=java")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java Developer"));
    }

    // ── Manage (officer) ──────────────────────────────────────────────────────

    @Test
    void listManage_asOfficer_returnsAllStatuses() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "ManageCorp");

        String draftId = createPosting(officer, companyId, "Draft Job");
        String openId = createPosting(officer, companyId, "Open Job");
        openPosting(officer, openId);

        mvc.perform(get("/api/job-postings/manage")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listManage_withStatusFilter_returnsOnlyMatchingStatus() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "FilterCorp");

        String draftId = createPosting(officer, companyId, "Draft Only Job");
        String openId = createPosting(officer, companyId, "Open Only Job");
        openPosting(officer, openId);

        mvc.perform(get("/api/job-postings/manage?status=DRAFT")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    void listManage_asStudent_returns403() throws Exception {
        TokenResponse student = registerStudent("student2@test.com");
        mvc.perform(get("/api/job-postings/manage")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isForbidden());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Test
    void getById_existingPosting_returns200WithSkillsAndBranches() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        TokenResponse student = registerStudent("student@test.com");
        String companyId = createCompany(officer, "DetailCorp");
        String postingId = createPosting(officer, companyId, "Full Stack Dev");

        mvc.perform(get("/api/job-postings/" + postingId)
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postingId))
                .andExpect(jsonPath("$.requiredSkills").isArray())
                .andExpect(jsonPath("$.eligibleBranches").isArray());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        TokenResponse student = registerStudent("student@test.com");
        mvc.perform(get("/api/job-postings/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isNotFound());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void update_draftPosting_returns200() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "UpdateCorp");
        String postingId = createPosting(officer, companyId, "Original Title");

        JobPostingUpdateRequest req = new JobPostingUpdateRequest(
                "Updated Title", "New description", new BigDecimal("10"), new BigDecimal("20"), null, 5);

        mvc.perform(put("/api/job-postings/" + postingId)
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.offerLimit").value(5));
    }

    @Test
    void update_openPosting_returns422() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "UpdateCorp2");
        String postingId = createPosting(officer, companyId, "Some Job");
        openPosting(officer, postingId);

        JobPostingUpdateRequest req = new JobPostingUpdateRequest("New Title", null, null, null, null, 1);

        mvc.perform(put("/api/job-postings/" + postingId)
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test
    void open_draftPosting_returns200WithOpenStatus() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "LifecycleCorp");
        String postingId = createPosting(officer, companyId, "Job to Open");

        mvc.perform(post("/api/job-postings/" + postingId + "/open")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void close_openPosting_returns200WithClosedStatus() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "LifecycleCorp2");
        String postingId = createPosting(officer, companyId, "Job to Close");
        openPosting(officer, postingId);

        mvc.perform(post("/api/job-postings/" + postingId + "/close")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void close_draftPosting_returns422() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "LifecycleCorp3");
        String postingId = createPosting(officer, companyId, "Draft to Close");

        mvc.perform(post("/api/job-postings/" + postingId + "/close")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancel_posting_returns200WithCancelledStatus() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "CancelCorp");
        String postingId = createPosting(officer, companyId, "Job to Cancel");

        mvc.perform(post("/api/job-postings/" + postingId + "/cancel")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    @Test
    void addSkill_returnsPostingWithSkill() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "SkillCorp");
        String postingId = createPosting(officer, companyId, "Dev Job");
        String skillId = createSkill(officer, "Kubernetes");

        mvc.perform(post("/api/job-postings/" + postingId + "/skills/" + skillId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredSkills[0].name").value("Kubernetes"));
    }

    @Test
    void removeSkill_returnsPostingWithSkillRemoved() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "SkillCorp2");
        String postingId = createPosting(officer, companyId, "Dev Job 2");
        String skillId = createSkill(officer, "Docker");

        // Add then remove
        mvc.perform(post("/api/job-postings/" + postingId + "/skills/" + skillId)
                .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/job-postings/" + postingId + "/skills/" + skillId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredSkills").isEmpty());
    }

    // ── Branches ──────────────────────────────────────────────────────────────

    @Test
    void addBranch_returnsPostingWithBranch() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "BranchCorp");
        String postingId = createPosting(officer, companyId, "CS Job");
        String branchId = createBranch(officer, "Computer Science", "CS");

        mvc.perform(post("/api/job-postings/" + postingId + "/branches/" + branchId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleBranches[0].name").value("Computer Science"));
    }

    @Test
    void removeBranch_returnsPostingWithBranchRemoved() throws Exception {
        TokenResponse officer = createOfficer("officer@test.com");
        String companyId = createCompany(officer, "BranchCorp2");
        String postingId = createPosting(officer, companyId, "CS Job 2");
        String branchId = createBranch(officer, "Electrical Engineering", "EE");

        mvc.perform(post("/api/job-postings/" + postingId + "/branches/" + branchId)
                .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/job-postings/" + postingId + "/branches/" + branchId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleBranches").isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final String TEST_PASSWORD = "password123";

    private TokenResponse createOfficer(String email) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(Role.ROLE_PLACEMENT_OFFICER);
        userRepo.save(user);
        return login(email);
    }

    private TokenResponse registerStudent(String email) throws Exception {
        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, TEST_PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private TokenResponse login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
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

    private String createPosting(TokenResponse officer, String companyId, String title) throws Exception {
        JobPostingCreateRequest req = new JobPostingCreateRequest(
                UUID.fromString(companyId), title, "Description",
                new BigDecimal("6"), new BigDecimal("12"), null, 2);
        MvcResult result = mvc.perform(post("/api/job-postings")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void openPosting(TokenResponse officer, String postingId) throws Exception {
        mvc.perform(post("/api/job-postings/" + postingId + "/open")
                .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());
    }

    private String createSkill(TokenResponse officer, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/skills")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillCreateRequest(name, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createBranch(TokenResponse officer, String name, String code) throws Exception {
        MvcResult result = mvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new BranchCreateRequest(name, code, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
