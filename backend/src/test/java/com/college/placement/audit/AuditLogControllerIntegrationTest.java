package com.college.placement.audit;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.audit.domain.AuditLog;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PR3 — Admin audit-log read API: authorization, pagination, filtering, DTO projection. */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String PASSWORD = "password123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired AuditLogRepository auditLogRepo;
    @Autowired OfferRepository offerRepo;
    @Autowired JobApplicationRepository applicationRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired PasswordEncoder passwordEncoder;

    // The H2 schema is reused across test classes, so a prior suite can leave rows that
    // FK-reference app_users (students, applications, …). Wipe the full dependency chain
    // before deleting users — mirrors CompanyControllerIntegrationTest — otherwise
    // userRepo.deleteAll() FK-violates under CI's class execution order.
    @BeforeEach
    void clean() {
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

    @AfterEach
    void tearDown() {
        // This suite is the only one that seeds audit_logs directly; clear it so the
        // rows do not perturb audit assertions in other test classes.
        auditLogRepo.deleteAll();
    }

    // ── Authorization ────────────────────────────────────────────────────────

    @Test
    void list_asAdmin_returns200_withPaginationMetadata() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("Student", "s1", "STUDENT_CREATED", "officer@x.test");
        saveLog("AppUser", "u1", "USER_DISABLED", "admin@x.test");

        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void list_asOfficer_returns403() throws Exception {
        String officer = tokenFor(verifiedUser("officer@audit.test", Role.ROLE_PLACEMENT_OFFICER));
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + officer))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_asStudent_returns403() throws Exception {
        String student = tokenFor(verifiedUser("student@audit.test", Role.ROLE_STUDENT));
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + student))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_anonymous_returns401() throws Exception {
        mvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    // ── Pagination ───────────────────────────────────────────────────────────

    @Test
    void list_pagination_splitsAcrossPages() throws Exception {
        String admin = adminToken("admin@audit.test");
        for (int i = 0; i < 3; i++) {
            saveLog("Student", "s" + i, "STUDENT_CREATED", "officer@x.test");
        }

        mvc.perform(get("/api/admin/audit-logs?page=0&size=2").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));

        mvc.perform(get("/api/admin/audit-logs?page=1&size=2").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(1));
    }

    // ── Filtering ────────────────────────────────────────────────────────────

    @Test
    void list_filterByAction() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("AppUser", "u1", "USER_DISABLED", "admin@x.test");
        saveLog("AppUser", "u2", "USER_ENABLED", "admin@x.test");

        mvc.perform(get("/api/admin/audit-logs?action=USER_DISABLED").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("USER_DISABLED"))
                .andExpect(jsonPath("$.content[0].entityId").value("u1"));
    }

    @Test
    void list_filterByEntityType() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("Student", "s1", "STUDENT_CREATED", "officer@x.test");
        saveLog("AppUser", "u1", "USER_DISABLED", "admin@x.test");

        mvc.perform(get("/api/admin/audit-logs?entityType=Student").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].entityType").value("Student"));
    }

    @Test
    void list_filterByEntityTypeAndEntityId() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("AppUser", "u1", "USER_DISABLED", "admin@x.test");
        saveLog("AppUser", "u2", "USER_ENABLED", "admin@x.test");

        mvc.perform(get("/api/admin/audit-logs?entityType=AppUser&entityId=u2")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].entityId").value("u2"));
    }

    @Test
    void list_filterByActor_caseInsensitiveContains() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("AppUser", "u1", "USER_DISABLED", "alice@corp.test");
        saveLog("AppUser", "u2", "USER_ENABLED", "system");

        mvc.perform(get("/api/admin/audit-logs?performedBy=ALICE").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].performedBy").value("alice@corp.test"));
    }

    @Test
    void list_filterByDateRange() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("Student", "s1", "STUDENT_CREATED", "officer@x.test");

        String from = Instant.now().minus(1, ChronoUnit.HOURS).toString();
        String to = Instant.now().plus(1, ChronoUnit.HOURS).toString();
        mvc.perform(get("/api/admin/audit-logs?dateFrom=" + from + "&dateTo=" + to)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // A window entirely in the future excludes the just-written row.
        String futureFrom = Instant.now().plus(1, ChronoUnit.HOURS).toString();
        mvc.perform(get("/api/admin/audit-logs?dateFrom=" + futureFrom)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void list_malformedDate_returns400() throws Exception {
        String admin = adminToken("admin@audit.test");
        mvc.perform(get("/api/admin/audit-logs?dateFrom=not-a-date").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_explicitSortByAction_ordersDeterministically() throws Exception {
        String admin = adminToken("admin@audit.test");
        saveLog("AppUser", "u2", "ACTION_B", "admin@x.test");
        saveLog("AppUser", "u3", "ACTION_C", "admin@x.test");
        saveLog("AppUser", "u1", "ACTION_A", "admin@x.test");

        mvc.perform(get("/api/admin/audit-logs?sort=action,asc").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("ACTION_A"))
                .andExpect(jsonPath("$.content[1].action").value("ACTION_B"))
                .andExpect(jsonPath("$.content[2].action").value("ACTION_C"));
    }

    // ── DTO projection / no entity leakage ───────────────────────────────────

    @Test
    void list_returnsDto_withoutInternalFields() throws Exception {
        String admin = adminToken("admin@audit.test");
        AuditLog seeded = saveLog("AppUser", "u1", "USER_DISABLED", "admin@x.test");
        seeded.setPreviousValue("ACTIVE");
        seeded.setNewValue("DISABLED");
        seeded.setReason("policy");
        auditLogRepo.saveAndFlush(seeded);

        mvc.perform(get("/api/admin/audit-logs?entityType=AppUser").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].action").value("USER_DISABLED"))
                .andExpect(jsonPath("$.content[0].performedBy").value("admin@x.test"))
                .andExpect(jsonPath("$.content[0].previousValue").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].newValue").value("DISABLED"))
                .andExpect(jsonPath("$.content[0].success").value(true))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                // Internal fields must not leak through the DTO projection.
                .andExpect(jsonPath("$.content[0].payload").doesNotExist())
                .andExpect(jsonPath("$.content[0].version").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AuditLog saveLog(String entityType, String entityId, String action, String performedBy) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setSuccess(true);
        return auditLogRepo.saveAndFlush(log);
    }

    private AppUser verifiedUser(String email, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setRole(role);
        u.setStatus(AccountStatus.ACTIVE);
        u.setEmailVerified(true);
        return userRepo.save(u);
    }

    private String adminToken(String email) throws Exception {
        return tokenFor(verifiedUser(email, Role.ROLE_ADMIN));
    }

    private String tokenFor(AppUser user) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(user.getEmail(), PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
