package com.college.placement.student;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.student.dto.SkillAliasRequest;
import com.college.placement.modules.student.dto.SkillCreateRequest;
import com.college.placement.modules.student.repository.SkillAliasRepository;
import com.college.placement.modules.student.repository.SkillRepository;
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

/** Master Skills Catalog endpoints: intelligent search, alias CRUD, detail-with-aliases. */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillCatalogControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SkillRepository skillRepo;
    @Autowired SkillAliasRepository skillAliasRepo;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void clean() {
        skillAliasRepo.deleteAll();
        skillRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    // ── Search ───────────────────────────────────────────────────────────────

    @Test
    void search_exactNameOutranksPartialMatch() throws Exception {
        TokenResponse officer = officer();
        createSkill(officer, "Java", "Programming Languages");
        createSkill(officer, "JavaScript", "Programming Languages");

        mvc.perform(get("/api/skills/search?q=java")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].matchType").value("EXACT"))
                .andExpect(jsonPath("$[1].name").value("JavaScript"))
                .andExpect(jsonPath("$[1].matchType").value("PARTIAL"));
    }

    @Test
    void search_findsSkillByAlias() throws Exception {
        TokenResponse officer = officer();
        String reactId = createSkill(officer, "React", "Web Frontend");
        addAlias(officer, reactId, "ReactJS");

        mvc.perform(get("/api/skills/search?q=reactjs")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("React"))
                .andExpect(jsonPath("$[0].matchType").value("ALIAS"));
    }

    @Test
    void search_findsSkillDespiteSpellingMistake() throws Exception {
        TokenResponse officer = officer();
        createSkill(officer, "Kubernetes", "DevOps");

        mvc.perform(get("/api/skills/search?q=kubernets")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kubernetes"))
                .andExpect(jsonPath("$[0].matchType").value("FUZZY"));
    }

    @Test
    void search_studentCanSearch() throws Exception {
        TokenResponse officer = officer();
        TokenResponse student = student();
        createSkill(officer, "Python", "Programming Languages");

        mvc.perform(get("/api/skills/search?q=python")
                        .header("Authorization", "Bearer " + student.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Python"));
    }

    @Test
    void search_emptyQueryReturnsEmptyList() throws Exception {
        TokenResponse officer = officer();

        mvc.perform(get("/api/skills/search?q=%20")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Aliases ──────────────────────────────────────────────────────────────

    @Test
    void addAlias_asOfficer_returns201_andDetailIncludesIt() throws Exception {
        TokenResponse officer = officer();
        String id = createSkill(officer, "Machine Learning", "AI & Machine Learning");

        mvc.perform(post("/api/skills/" + id + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest("ML"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value("ML"))
                .andExpect(jsonPath("$.skillId").value(id));

        mvc.perform(get("/api/skills/" + id)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aliases[0]").value("ML"));
    }

    @Test
    void addAlias_asStudent_returns403() throws Exception {
        TokenResponse officer = officer();
        TokenResponse student = student();
        String id = createSkill(officer, "Docker", "DevOps");

        mvc.perform(post("/api/skills/" + id + "/aliases")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest("Containers"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addAlias_duplicate_returns409() throws Exception {
        TokenResponse officer = officer();
        String reactId = createSkill(officer, "React", "Web Frontend");
        String vueId = createSkill(officer, "Vue.js", "Web Frontend");
        addAlias(officer, reactId, "ReactJS");

        // Same alias on another skill → conflict (normalized uniqueness is global).
        mvc.perform(post("/api/skills/" + vueId + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest("reactjs"))))
                .andExpect(status().isConflict());
    }

    @Test
    void addAlias_clashingWithSkillName_returns409() throws Exception {
        TokenResponse officer = officer();
        createSkill(officer, "Java", "Programming Languages");
        String id = createSkill(officer, "Spring Boot", "Backend Frameworks");

        mvc.perform(post("/api/skills/" + id + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest("java"))))
                .andExpect(status().isConflict());
    }

    @Test
    void removeAlias_returns204_andAliasStopsResolving() throws Exception {
        TokenResponse officer = officer();
        String id = createSkill(officer, "Go", "Programming Languages");
        MvcResult created = mvc.perform(post("/api/skills/" + id + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest("Golang"))))
                .andExpect(status().isCreated())
                .andReturn();
        String aliasId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(delete("/api/skills/" + id + "/aliases/" + aliasId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/skills/" + id + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void removeAlias_wrongSkill_returns404() throws Exception {
        TokenResponse officer = officer();
        String reactId = createSkill(officer, "React", "Web Frontend");
        String vueId = createSkill(officer, "Vue.js", "Web Frontend");
        MvcResult created = mvc.perform(post("/api/skills/" + reactId + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest("ReactJS"))))
                .andExpect(status().isCreated())
                .andReturn();
        String aliasId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(delete("/api/skills/" + vueId + "/aliases/" + aliasId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void existingCreateEndpoint_returnsNewCatalogFields() throws Exception {
        TokenResponse officer = officer();

        mvc.perform(post("/api/skills")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillCreateRequest("Rust", "Programming Languages"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.popularityScore").value(0))
                .andExpect(jsonPath("$.createdSource").value("MANUAL"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private TokenResponse officer() throws Exception {
        return login("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
    }

    private TokenResponse student() throws Exception {
        return login("student@test.com", Role.ROLE_STUDENT);
    }

    private TokenResponse login(String email, Role role) throws Exception {
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

    private void addAlias(TokenResponse officer, String skillId, String alias) throws Exception {
        mvc.perform(post("/api/skills/" + skillId + "/aliases")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new SkillAliasRequest(alias))))
                .andExpect(status().isCreated());
    }
}
