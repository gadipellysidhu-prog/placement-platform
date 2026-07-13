package com.college.placement.jobintelligence;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import com.college.placement.modules.jobintelligence.dto.StartRunRequest;
import com.college.placement.modules.jobintelligence.repository.ExtractionCacheRepository;
import com.college.placement.modules.jobintelligence.repository.JobIntelligenceRunRepository;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.SkillCreatedSource;
import com.college.placement.modules.student.repository.BranchRepository;
import com.college.placement.modules.student.repository.SkillAliasRepository;
import com.college.placement.modules.student.repository.SkillRepository;
import com.college.placement.support.DatabaseCleaner;
import com.college.placement.shared.settings.domain.SettingValueType;
import com.college.placement.shared.settings.service.SettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end pipeline test: officer starts a run → async pipeline fetches a local
 * fixture page → stub LLM extracts skills → catalog matched/evolved → posting
 * tagged through the sanctioned service path → branches predicted → run COMPLETED.
 */
@SpringBootTest(classes = com.college.placement.Application.class,
        properties = {
                "job.intelligence.crawler.allow-private-networks=true",
                "job.intelligence.ai.provider=stub"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobIntelligencePipelineIntegrationTest {

    private static final String JOB_HTML = """
            <html><head><script>tracking()</script></head><body>
              <nav>Company | Careers</nav>
              <main>
                <h1>Embedded Software Engineer</h1>
                <p>We are looking for engineers with Embedded C, STM32, UART and CAN
                   experience. Knowledge of FreeRTOS, Docker and Python is a plus.
                   Strong Communication skills required.</p>
              </main>
            </body></html>
            """;

    private static HttpServer fixtureServer;
    private static final AtomicInteger fetchCount = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired DatabaseCleaner databaseCleaner;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired SkillRepository skillRepo;
    @Autowired SkillAliasRepository skillAliasRepo;
    @Autowired BranchRepository branchRepo;
    @Autowired JobIntelligenceRunRepository runRepo;
    @Autowired ExtractionCacheRepository cacheRepo;
    @Autowired SettingsService settingsService;

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String TEST_PASSWORD = "password123";

    @BeforeAll
    static void startFixtureServer() throws Exception {
        fixtureServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fixtureServer.createContext("/job", exchange -> {
            fetchCount.incrementAndGet();
            byte[] body = JOB_HTML.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        fixtureServer.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        fixtureServer.start();
    }

    @AfterAll
    static void stopFixtureServer() {
        fixtureServer.stop(0);
    }

    @BeforeEach
    void clean() {
        // FK-safe user/student graph first (clears applications → releases job postings,
        // and students → releases the student_skills join before skills are deleted).
        databaseCleaner.clean();
        runRepo.deleteAll();
        cacheRepo.deleteAll();
        jobPostingRepo.deleteAll();
        companyRepo.deleteAll();
        skillAliasRepo.deleteAll();
        skillRepo.deleteAll();
        branchRepo.deleteAll();
        settingsService.evictCache();
        settingsService.upsert("job.intelligence.enabled", "true", SettingValueType.BOOLEAN,
                "job-intelligence", "test", null);
    }

    private String fixtureUrl(String path) {
        return "http://127.0.0.1:" + fixtureServer.getAddress().getPort() + path;
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    void fullPipeline_tagsSkillsPredictsBranchesAndCompletes() throws Exception {
        TokenResponse officer = officer();
        seedCatalog();
        UUID postingId = createDraftPosting();

        MvcResult started = mvc.perform(post("/api/job-intelligence/runs")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new StartRunRequest(postingId, fixtureUrl("/job")))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        UUID runId = UUID.fromString(
                mapper.readTree(started.getResponse().getContentAsString()).get("id").asText());

        awaitTerminal(officer, runId);

        // Run row reflects the completed pipeline.
        mvc.perform(get("/api/job-intelligence/runs/" + runId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.provider").value("stub"))
                .andExpect(jsonPath("$.skillsExtracted", org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.skillsTagged", org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.predictedBranches", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));

        // Posting was tagged through the sanctioned path: detail shows skills + branches.
        mvc.perform(get("/api/job-postings/" + postingId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredSkills", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())))
                .andExpect(jsonPath("$.eligibleBranches", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));

        // Continuous learning: "Docker" was not seeded → created with source=AI.
        assertThat(skillRepo.findByNameIgnoreCase("docker"))
                .hasValueSatisfying(skill ->
                        assertThat(skill.getCreatedSource()).isEqualTo(SkillCreatedSource.AI));

        // Extraction cached for this URL.
        assertThat(cacheRepo.count()).isEqualTo(1);
    }

    @Test
    void secondRunOnSameUrl_reusesCachedExtraction() throws Exception {
        TokenResponse officer = officer();
        seedCatalog();
        UUID posting1 = createDraftPosting();
        UUID posting2 = createDraftPosting();
        int fetchesBefore = fetchCount.get();

        UUID run1 = startRun(officer, posting1, fixtureUrl("/job"));
        awaitTerminal(officer, run1);
        UUID run2 = startRun(officer, posting2, fixtureUrl("/job"));
        awaitTerminal(officer, run2);

        assertThat(runRepo.findById(run2).orElseThrow().getStatus()).isEqualTo(RunStatus.COMPLETED);
        // Only the first run touched the network.
        assertThat(fetchCount.get() - fetchesBefore).isEqualTo(1);
    }

    // ── Failure + retry ──────────────────────────────────────────────────────

    @Test
    void notFoundPage_failsRun_andRetryIsPossible() throws Exception {
        TokenResponse officer = officer();
        seedCatalog();
        UUID postingId = createDraftPosting();

        UUID runId = startRun(officer, postingId, fixtureUrl("/missing"));
        awaitTerminal(officer, runId);

        mvc.perform(get("/api/job-intelligence/runs/" + runId)
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage", org.hamcrest.Matchers.containsString("404")));

        // Retry re-enters the pipeline on the same row (still failing — same URL).
        mvc.perform(post("/api/job-intelligence/runs/" + runId + "/retry")
                        .header("Authorization", "Bearer " + officer.accessToken()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.retryCount").value(1));
        awaitTerminal(officer, runId);
        assertThat(runRepo.findById(runId).orElseThrow().getStatus()).isEqualTo(RunStatus.FAILED);
    }

    // ── Guard rails ──────────────────────────────────────────────────────────

    @Test
    void featureFlagOff_rejectsNewRuns() throws Exception {
        TokenResponse officer = officer();
        UUID postingId = createDraftPosting();
        settingsService.upsert("job.intelligence.enabled", "false", SettingValueType.BOOLEAN,
                "job-intelligence", "test", null);

        mvc.perform(post("/api/job-intelligence/runs")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new StartRunRequest(postingId, fixtureUrl("/job")))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void invalidUrl_isRejectedUpFront() throws Exception {
        TokenResponse officer = officer();
        UUID postingId = createDraftPosting();

        mvc.perform(post("/api/job-intelligence/runs")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new StartRunRequest(postingId, "ftp://example.com/job"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void studentsCannotStartRuns() throws Exception {
        TokenResponse student = login("student@test.com", Role.ROLE_STUDENT);
        UUID postingId = createDraftPosting();

        mvc.perform(post("/api/job-intelligence/runs")
                        .header("Authorization", "Bearer " + student.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new StartRunRequest(postingId, fixtureUrl("/job")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentRunForSamePosting_returns409() throws Exception {
        TokenResponse officer = officer();
        seedCatalog();
        UUID postingId = createDraftPosting();
        UUID runId = startRun(officer, postingId, fixtureUrl("/job"));

        // Immediately racing a second start may hit the still-active first run;
        // whichever way the race resolves, the API must never 5xx.
        MvcResult second = mvc.perform(post("/api/job-intelligence/runs")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(
                                new StartRunRequest(postingId, fixtureUrl("/job")))))
                .andReturn();
        assertThat(second.getResponse().getStatus()).isIn(202, 409);

        awaitTerminal(officer, runId);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void awaitTerminal(TokenResponse officer, UUID runId) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            RunStatus status = runRepo.findById(runId).orElseThrow().getStatus();
            if (status.isTerminal()) {
                return;
            }
            Thread.sleep(150);
        }
        throw new AssertionError("Run " + runId + " did not reach a terminal state in time");
    }

    private UUID startRun(TokenResponse officer, UUID postingId, String url) throws Exception {
        MvcResult result = mvc.perform(post("/api/job-intelligence/runs")
                        .header("Authorization", "Bearer " + officer.accessToken())
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new StartRunRequest(postingId, url))))
                .andExpect(status().isAccepted())
                .andReturn();
        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void seedCatalog() {
        for (String[] entry : new String[][] {
                {"Embedded C", "Embedded Systems"}, {"STM32", "Embedded Systems"},
                {"UART", "Embedded Systems"}, {"CAN", "Embedded Systems"},
                {"FreeRTOS", "Embedded Systems"}, {"Python", "Programming Languages"},
                {"Communication", "Soft Skills"}}) {
            Skill skill = new Skill();
            skill.setName(entry[0]);
            skill.setCategory(entry[1]);
            skill.setCreatedSource(SkillCreatedSource.SEED);
            skillRepo.save(skill);
        }
        Branch ece = new Branch();
        ece.setName("Electronics & Communication");
        ece.setCode("ECE");
        branchRepo.save(ece);
        Branch cse = new Branch();
        cse.setName("Computer Science");
        cse.setCode("CSE");
        branchRepo.save(cse);
    }

    private UUID createDraftPosting() {
        Company company = companyRepo.findAll().stream().findFirst().orElseGet(() -> {
            Company c = new Company();
            c.setName("Acme Corp " + UUID.randomUUID());
            return companyRepo.save(c);
        });
        JobPosting posting = new JobPosting();
        posting.setCompany(company);
        posting.setTitle("Embedded Software Engineer");
        posting.setDescription("draft");
        posting.setCtcMin(new BigDecimal("6"));
        posting.setCtcMax(new BigDecimal("10"));
        posting.setOfferLimit(2);
        return jobPostingRepo.save(posting).getId();
    }

    private TokenResponse officer() throws Exception {
        return login("officer@test.com", Role.ROLE_PLACEMENT_OFFICER);
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
}
