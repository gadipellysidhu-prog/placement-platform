package com.college.placement.student;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.dto.BranchCreateRequest;
import com.college.placement.modules.student.dto.BranchUpdateRequest;
import com.college.placement.modules.student.repository.BranchRepository;
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
class BranchControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired BranchRepository branchRepo;
    @Autowired OfferRepository offerRepo;
    @Autowired JobApplicationRepository applicationRepo;
    @Autowired CertificateRepository certRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired CompanyRepository companyRepo;

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
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void create_asOfficer_returns201() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        BranchCreateRequest req = new BranchCreateRequest("Computer Science", "CS", "CS branch");

        mvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Computer Science"))
                .andExpect(jsonPath("$.code").value("CS"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_asStudent_returns403() throws Exception {
        TokenResponse student = registerStudent("student@test.com");
        BranchCreateRequest req = new BranchCreateRequest("IT", "IT", null);

        mvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        createBranch(officer, "Mechanical", "ME");

        mvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new BranchCreateRequest("Mechanical", "ME2", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void list_returnsList() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = registerStudent("student@test.com");
        createBranch(officer, "Electronics", "ECE");

        mvc.perform(get("/api/branches")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        TokenResponse student = registerStudent("student@test.com");

        mvc.perform(get("/api/branches/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_asOfficer_returns200() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createBranch(officer, "OldBranch", "OB");

        mvc.perform(put("/api/branches/" + id)
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new BranchUpdateRequest("NewBranch", "NB", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewBranch"));
    }

    @Test
    void activate_returns200() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createBranch(officer, "ActivateBranch", "AB");

        // Deactivate first
        mvc.perform(post("/api/branches/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Then activate
        mvc.perform(post("/api/branches/" + id + "/activate")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivate_returns200() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createBranch(officer, "DeactivateBranch", "DB");

        mvc.perform(post("/api/branches/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // Helpers

    private TokenResponse registerStudent(String email) throws Exception {
        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, TEST_PASSWORD, Role.ROLE_STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private TokenResponse createPrivilegedUser(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        userRepo.save(user);
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
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
