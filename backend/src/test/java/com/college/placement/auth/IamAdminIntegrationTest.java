package com.college.placement.auth;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.domain.VerificationToken;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.auth.repository.VerificationTokenRepository;
import com.college.placement.modules.notification.domain.NotificationHistory;
import com.college.placement.modules.notification.repository.NotificationHistoryRepository;
import com.college.placement.shared.audit.domain.AuditLog;
import com.college.placement.shared.audit.repository.AuditLogRepository;
import com.college.placement.support.DatabaseCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase D — IAM: user administration, roles, invitations, account-state login enforcement. */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IamAdminIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String PASSWORD = "password123";
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired VerificationTokenRepository tokenRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired DatabaseCleaner databaseCleaner;
    @Autowired NotificationHistoryRepository notificationRepo;
    @Autowired AuditLogRepository auditLogRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        tokenRepo.deleteAll();
        notificationRepo.deleteAll();
        auditLogRepo.deleteAll();
        databaseCleaner.clean();
    }

    // ── Invitations ──────────────────────────────────────────────────────────

    @Test
    void invite_acceptInvitation_activatesAccount_andLoginWorks() throws Exception {
        String admin = adminToken("admin@iam.test");
        String invitee = "officer@iam.test";

        mvc.perform(post("/api/admin/users/invite").header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content(inviteJson(invitee, Role.ROLE_PLACEMENT_OFFICER)))
                .andExpect(status().isAccepted());

        AppUser created = userRepo.findByEmail(invitee).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(AccountStatus.INVITED);

        String token = latestToken(invitee, "You have been invited to Placement Platform");
        mvc.perform(post("/auth/accept-invitation").contentType(JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword456\"}"))
                .andExpect(status().isOk());

        AppUser activated = userRepo.findByEmail(invitee).orElseThrow();
        assertThat(activated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(activated.isEmailVerified()).isTrue();

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(invitee, "newpassword456"))))
                .andExpect(status().isOk());
    }

    @Test
    void acceptInvitation_replay_isRejected() throws Exception {
        String admin = adminToken("admin@iam.test");
        String invitee = "officer2@iam.test";
        mvc.perform(post("/api/admin/users/invite").header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content(inviteJson(invitee, Role.ROLE_PLACEMENT_OFFICER)))
                .andExpect(status().isAccepted());
        String token = latestToken(invitee, "You have been invited to Placement Platform");

        mvc.perform(post("/auth/accept-invitation").contentType(JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword456\"}"))
                .andExpect(status().isOk());
        // Replay: same token cannot be reused.
        mvc.perform(post("/auth/accept-invitation").contentType(JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"another12345\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptInvitation_expiredToken_isRejected() throws Exception {
        String admin = adminToken("admin@iam.test");
        String invitee = "officer3@iam.test";
        mvc.perform(post("/api/admin/users/invite").header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content(inviteJson(invitee, Role.ROLE_PLACEMENT_OFFICER)))
                .andExpect(status().isAccepted());
        String token = latestToken(invitee, "You have been invited to Placement Platform");

        VerificationToken vt = tokenRepo.findAll().stream()
                .filter(t -> !t.isConsumed()).findFirst().orElseThrow();
        vt.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        tokenRepo.saveAndFlush(vt);

        mvc.perform(post("/auth/accept-invitation").contentType(JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword456\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── Account state + login enforcement ────────────────────────────────────

    @Test
    void disable_thenLogin_rejected_andEnableRestores() throws Exception {
        String admin = adminToken("admin@iam.test");
        AppUser target = verifiedUser("target@iam.test", Role.ROLE_STUDENT);

        mvc.perform(post("/api/admin/users/" + target.getId() + "/disable")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("target@iam.test", PASSWORD))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/users/" + target.getId() + "/enable")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("target@iam.test", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void lock_thenLogin_rejected_andUnlockRestores() throws Exception {
        String admin = adminToken("admin@iam.test");
        AppUser target = verifiedUser("locktarget@iam.test", Role.ROLE_STUDENT);

        mvc.perform(post("/api/admin/users/" + target.getId() + "/lock")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("locktarget@iam.test", PASSWORD))))
                .andExpect(status().isLocked());

        mvc.perform(post("/api/admin/users/" + target.getId() + "/unlock")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── Roles ────────────────────────────────────────────────────────────────

    @Test
    void assignRole_changesRole_andListsInGetUser() throws Exception {
        String admin = adminToken("admin@iam.test");
        AppUser target = verifiedUser("roletarget@iam.test", Role.ROLE_STUDENT);

        mvc.perform(put("/api/admin/users/" + target.getId() + "/role")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content("{\"role\":\"ROLE_PLACEMENT_OFFICER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_PLACEMENT_OFFICER"));

        mvc.perform(get("/api/admin/users/" + target.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_PLACEMENT_OFFICER"));
    }

    @Test
    void cannotDisableOrDemote_lastActiveAdmin() throws Exception {
        String admin = adminToken("admin@iam.test");
        UUID adminId = userRepo.findByEmail("admin@iam.test").orElseThrow().getId();

        mvc.perform(post("/api/admin/users/" + adminId + "/disable").header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/users/" + adminId + "/role").header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content("{\"role\":\"ROLE_STUDENT\"}"))
                .andExpect(status().isConflict());
    }

    // ── Last activity ────────────────────────────────────────────────────────

    @Test
    void lastActivityAt_isDerivedFromLogin_andNullWhenNoActivityRecorded() throws Exception {
        // adminToken() logs in, which mints a refresh token — the activity record.
        String admin = adminToken("admin@iam.test");
        // Created directly, never authenticated: no activity exists for this account.
        verifiedUser("dormant@iam.test", Role.ROLE_STUDENT);

        mvc.perform(get("/api/admin/users").param("query", "admin@iam.test")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastActivityAt").isNotEmpty());

        // Reported as unknown rather than back-filled from createdAt/updatedAt.
        mvc.perform(get("/api/admin/users").param("query", "dormant@iam.test")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastActivityAt").value(nullValue()));
    }

    // ── Authorization + audit + events ───────────────────────────────────────

    @Test
    void nonAdmin_cannotAccessAdminApis() throws Exception {
        String studentToken = tokenFor(verifiedUser("plainstudent@iam.test", Role.ROLE_STUDENT).getEmail());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAction_writesAudit_andPublishesEvent() throws Exception {
        String admin = adminToken("admin@iam.test");
        AppUser target = verifiedUser("audittarget@iam.test", Role.ROLE_STUDENT);

        mvc.perform(post("/api/admin/users/" + target.getId() + "/disable").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        var logs = auditLogRepo.findByEntityTypeAndEntityId("AppUser", target.getId().toString(),
                PageRequest.of(0, 50)).getContent();
        // AuditService action + the event-driven audit entry.
        assertThat(logs).anySatisfy(l -> assertThat(l.getAction()).isEqualTo("USER_DISABLED"));
        assertThat(logs).anySatisfy(l -> assertThat(l.getAction()).isEqualTo("UserDisabledEvent"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String inviteJson(String email, Role role) throws Exception {
        return "{\"email\":\"" + email + "\",\"role\":\"" + role.name() + "\"}";
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
        verifiedUser(email, Role.ROLE_ADMIN);
        return tokenFor(email);
    }

    private String tokenFor(String email) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String latestToken(String email, String subject) {
        NotificationHistory history = notificationRepo
                .findByUser(userRepo.findByEmail(email).orElseThrow(), PageRequest.of(0, 20))
                .getContent().stream()
                .filter(h -> subject.equals(h.getSubject()))
                .max(Comparator.comparing(NotificationHistory::getCreatedAt))
                .orElseThrow(() -> new AssertionError("No notification with subject: " + subject));
        Matcher m = TOKEN.matcher(history.getBody());
        assertThat(m.find()).as("token present in email body").isTrue();
        return m.group(1);
    }
}
