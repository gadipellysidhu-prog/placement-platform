package com.college.placement.analytics;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.domain.JobApplication;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Analytics module — placement reporting aggregates, authorization, and empty-dataset behaviour. */
@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String PASSWORD = "password123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired BranchRepository branchRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired JobPostingRepository jobPostingRepo;
    @Autowired JobApplicationRepository applicationRepo;

    private final AtomicInteger rollSeq = new AtomicInteger(1000);

    @BeforeEach
    void clean() {
        applicationRepo.deleteAll();
        studentRepo.deleteAll();
        jobPostingRepo.deleteAll();
        companyRepo.deleteAll();
        branchRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Test
    void analytics_asStudent_returns403() throws Exception {
        String student = tokenFor(user("student@analytics.test", Role.ROLE_STUDENT));
        mvc.perform(get("/api/analytics/overview").header("Authorization", "Bearer " + student))
                .andExpect(status().isForbidden());
    }

    @Test
    void overview_empty_returnsZeros() throws Exception {
        String officer = tokenFor(user("officer@analytics.test", Role.ROLE_PLACEMENT_OFFICER));
        mvc.perform(get("/api/analytics/overview").header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(0))
                .andExpect(jsonPath("$.placedStudents").value(0))
                .andExpect(jsonPath("$.placementRatePercent").value(0.0))
                .andExpect(jsonPath("$.offerConversionPercent").value(0.0));
    }

    // ── Aggregates over a seeded dataset ───────────────────────────────────────

    @Test
    void report_overSeededData_computesExpectedAggregates() throws Exception {
        String officer = tokenFor(user("officer2@analytics.test", Role.ROLE_PLACEMENT_OFFICER));

        Branch cse = branch("CSE");
        Branch ece = branch("ECE");

        // CSE: 3 students, 2 placed. ECE: 2 students, 0 placed. Overall 2/5 = 40%.
        Student cse1 = student(cse, StudentStatus.PLACED);
        Student cse2 = student(cse, StudentStatus.PLACED);
        Student cse3 = student(cse, StudentStatus.ACTIVE);
        Student ece1 = student(ece, StudentStatus.ACTIVE);
        Student ece2 = student(ece, StudentStatus.ACTIVE);

        Company acme = company("Acme");
        JobPosting posting = posting(acme, new BigDecimal("10.00"), new BigDecimal("20.00"));

        // 5 applications, one per student: 2 OFFERED, 1 SHORTLISTED, 1 APPLIED, 1 REJECTED.
        application(cse1, posting, ApplicationStatus.OFFERED);
        application(cse2, posting, ApplicationStatus.OFFERED);
        application(cse3, posting, ApplicationStatus.SHORTLISTED);
        application(ece1, posting, ApplicationStatus.APPLIED);
        application(ece2, posting, ApplicationStatus.REJECTED);

        String auth = "Bearer " + officer;
        String month = YearMonth.now(ZoneOffset.UTC).toString();

        mvc.perform(get("/api/analytics/report").header("Authorization", auth))
                .andExpect(status().isOk())
                // overview
                .andExpect(jsonPath("$.overview.totalStudents").value(5))
                .andExpect(jsonPath("$.overview.placedStudents").value(2))
                .andExpect(jsonPath("$.overview.placementRatePercent").value(40.0))
                .andExpect(jsonPath("$.overview.totalApplications").value(5))
                .andExpect(jsonPath("$.overview.totalOffers").value(2))
                .andExpect(jsonPath("$.overview.offerConversionPercent").value(40.0))
                .andExpect(jsonPath("$.overview.activeCompanies").value(1))
                .andExpect(jsonPath("$.overview.openPostings").value(1))
                // funnel (all six statuses present, zero-filled)
                .andExpect(jsonPath("$.funnel.total").value(5))
                .andExpect(jsonPath("$.funnel.byStatus.OFFERED").value(2))
                .andExpect(jsonPath("$.funnel.byStatus.APPLIED").value(1))
                .andExpect(jsonPath("$.funnel.byStatus.SHORTLISTED").value(1))
                .andExpect(jsonPath("$.funnel.byStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.funnel.byStatus.INTERVIEWED").value(0))
                .andExpect(jsonPath("$.funnel.byStatus.WITHDRAWN").value(0))
                // top recruiters
                .andExpect(jsonPath("$.topRecruiters[0].company").value("Acme"))
                .andExpect(jsonPath("$.topRecruiters[0].offers").value(2))
                .andExpect(jsonPath("$.topRecruiters[0].totalApplications").value(5))
                // CTC
                .andExpect(jsonPath("$.ctc.minCtc").value(10.00))
                .andExpect(jsonPath("$.ctc.maxCtc").value(20.00))
                .andExpect(jsonPath("$.ctc.avgCtc").value(20.00))
                .andExpect(jsonPath("$.ctc.postingsConsidered").value(1))
                // trend — 6-month window; the last bucket (index 5) is the current month
                .andExpect(jsonPath("$.trend.length()").value(6))
                .andExpect(jsonPath("$.trend[5].month").value(month))
                .andExpect(jsonPath("$.trend[5].applications").value(5))
                .andExpect(jsonPath("$.trend[5].offers").value(2));
    }

    @Test
    void byBranch_computesPerBranchPlacementRates() throws Exception {
        String officer = tokenFor(user("officer3@analytics.test", Role.ROLE_PLACEMENT_OFFICER));
        Branch cse = branch("CSE");
        student(cse, StudentStatus.PLACED);
        student(cse, StudentStatus.PLACED);
        student(cse, StudentStatus.ACTIVE);

        mvc.perform(get("/api/analytics/by-branch").header("Authorization", "Bearer " + officer))
                .andExpect(status().isOk())
                // only CSE students seeded → single branch bucket at index 0
                .andExpect(jsonPath("$[0].branch").value("CSE"))
                .andExpect(jsonPath("$[0].totalStudents").value(3))
                .andExpect(jsonPath("$[0].placedStudents").value(2))
                // 2/3 => 66.7 (one decimal, HALF_UP)
                .andExpect(jsonPath("$[0].placementRatePercent").value(66.7));
    }

    // ── seeding helpers ────────────────────────────────────────────────────────

    private AppUser user(String email, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmailVerified(true);
        return userRepo.save(u);
    }

    private String tokenFor(AppUser user) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(user.getEmail(), PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Branch branch(String name) {
        Branch b = new Branch();
        b.setName(name);
        b.setCode(name);
        return branchRepo.save(b);
    }

    private Student student(Branch branch, StudentStatus status) {
        int seq = rollSeq.getAndIncrement();
        AppUser u = user("stu" + seq + "@analytics.test", Role.ROLE_STUDENT);
        Student s = new Student();
        s.setUser(u);
        s.setBranch(branch);
        s.setRollNumber("R" + seq);
        s.setCurrentYear(4);
        s.setStatus(status);
        return studentRepo.save(s);
    }

    private Company company(String name) {
        Company c = new Company();
        c.setName(name);
        return companyRepo.save(c);
    }

    private JobPosting posting(Company company, BigDecimal ctcMin, BigDecimal ctcMax) {
        JobPosting p = new JobPosting();
        p.setCompany(company);
        p.setTitle("Software Engineer");
        p.setCtcMin(ctcMin);
        p.setCtcMax(ctcMax);
        p.setStatus(JobPostingStatus.OPEN);
        return jobPostingRepo.save(p);
    }

    private JobApplication application(Student student, JobPosting posting, ApplicationStatus status) {
        JobApplication a = new JobApplication();
        a.setStudent(student);
        a.setJobPosting(posting);
        a.setStatus(status);
        return applicationRepo.save(a);
    }
}
