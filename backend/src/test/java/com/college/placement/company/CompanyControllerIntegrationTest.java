package com.college.placement.company;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.company.dto.CompanyCreateRequest;
import com.college.placement.modules.company.dto.CompanyUpdateRequest;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepo;
    @Autowired com.college.placement.modules.placement.repository.OfferRepository offerRepo;
    @Autowired com.college.placement.modules.placement.repository.JobApplicationRepository applicationRepo;
    @Autowired com.college.placement.modules.certificate.repository.CertificateRepository certRepo;
    @Autowired com.college.placement.modules.student.repository.StudentRepository studentRepo;
    @Autowired com.college.placement.modules.company.repository.JobPostingRepository jobPostingRepo;

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
    void createCompany_asOfficer_returns201() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        CompanyCreateRequest req = new CompanyCreateRequest("TechCorp", "https://techcorp.com", "Technology", "A tech company");

        mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TechCorp"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCompany_asStudent_returns403() throws Exception {
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        CompanyCreateRequest req = new CompanyCreateRequest("TechCorp", null, null, null);

        mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCompany_unauthorized_returns401() throws Exception {
        mvc.perform(post("/api/companies")
                        .contentType(JSON)
                        .content("{\"name\":\"TechCorp\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCompany_duplicateName_returns409() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        createCompany(officer, "DuplicateCorp");

        mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new CompanyCreateRequest("DuplicateCorp", null, null, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void listCompanies_asStudent_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        TokenResponse student = register("student@test.com", Role.ROLE_STUDENT);
        createCompany(officer, "ListCorp");

        mvc.perform(get("/api/companies")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getCompanyById_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createCompany(officer, "GetCorp");

        mvc.perform(get("/api/companies/" + id)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("GetCorp"));
    }

    @Test
    void getCompanyById_notFound_returns404() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);

        mvc.perform(get("/api/companies/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCompany_asOfficer_returns200() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createCompany(officer, "OldName");

        CompanyUpdateRequest upd = new CompanyUpdateRequest("NewName", null, "Finance", null, null);
        mvc.perform(put("/api/companies/" + id)
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void blacklistCompany_asAdmin_returns200() throws Exception {
        TokenResponse admin = register("admin@test.com", Role.ROLE_ADMIN);
        String id = createCompany(admin, "BadCorp");

        mvc.perform(post("/api/companies/" + id + "/blacklist")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLACKLISTED"));
    }

    @Test
    void blacklistCompany_asOfficer_returns403() throws Exception {
        TokenResponse officer = register("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
        String id = createCompany(officer, "SomeCorp");

        mvc.perform(post("/api/companies/" + id + "/blacklist")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isForbidden());
    }

    // Helpers

    private static final String TEST_PASSWORD = "password123";

    private TokenResponse register(String email, Role role) throws Exception {
        if (role == Role.ROLE_STUDENT) {
            MvcResult result = mvc.perform(post("/auth/register")
                            .contentType(JSON)
                            .content(mapper.writeValueAsString(new com.college.placement.modules.auth.dto.RegisterRequest(email, TEST_PASSWORD, role))))
                    .andExpect(status().isCreated())
                    .andReturn();
            return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
        }
        return createPrivilegedUser(email, role);
    }

    private TokenResponse createPrivilegedUser(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
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

    private String createCompany(TokenResponse token, String name) throws Exception {
        CompanyCreateRequest req = new CompanyCreateRequest(name, null, "Technology", null);
        MvcResult result = mvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + token.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
