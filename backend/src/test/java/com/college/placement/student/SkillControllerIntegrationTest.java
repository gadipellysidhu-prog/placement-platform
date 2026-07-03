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
import com.college.placement.modules.student.dto.SkillCreateRequest;
import com.college.placement.modules.student.dto.SkillUpdateRequest;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SkillRepository skillRepo;
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
        skillRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    void create_asOfficer_returns201() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(post("/api/skills")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillCreateRequest("Java", "Programming"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.category").value("Programming"))
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void create_asStudent_returns403() throws Exception {
        TokenResponse student = registerStudent("student@test.com");

        mvc.perform(post("/api/skills")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillCreateRequest("Python", "Programming"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        createSkill(officer, "React", "Frontend");

        mvc.perform(post("/api/skills")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillCreateRequest("React", "Frontend"))))
                .andExpect(status().isConflict());
    }

    @Test
    void list_returnsList() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = registerStudent("student@test.com");
        createSkill(officer, "Spring Boot", "Framework");

        mvc.perform(get("/api/skills")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Spring Boot"));
    }

    @Test
    void list_filteredByCategory() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = registerStudent("student@test.com");
        createSkill(officer, "Docker", "DevOps");
        createSkill(officer, "SQL", "Database");

        mvc.perform(get("/api/skills?category=DevOps")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Docker"));
    }

    @Test
    void list_verifiedOnly() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = registerStudent("student@test.com");
        String skillId = createSkill(officer, "Kubernetes", "DevOps");
        createSkill(officer, "Ansible", "DevOps");

        // Verify only the first skill
        mvc.perform(post("/api/skills/" + skillId + "/verify")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/skills?verified=true")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Kubernetes"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        TokenResponse student = registerStudent("student@test.com");

        mvc.perform(get("/api/skills/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns200() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createSkill(officer, "OldSkill", "OldCat");

        mvc.perform(put("/api/skills/" + id)
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillUpdateRequest("NewSkill", "NewCat"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewSkill"))
                .andExpect(jsonPath("$.category").value("NewCat"));
    }

    @Test
    void verify_returns200() throws Exception {
        TokenResponse officer = createPrivilegedUser("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createSkill(officer, "AWS", "Cloud");

        mvc.perform(post("/api/skills/" + id + "/verify")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
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

    private String createSkill(TokenResponse officer, String name, String category) throws Exception {
        MvcResult result = mvc.perform(post("/api/skills")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillCreateRequest(name, category))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
