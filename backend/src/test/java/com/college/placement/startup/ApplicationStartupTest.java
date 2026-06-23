package com.college.placement.startup;

import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.company.repository.RecruiterRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.repository.BranchRepository;
import com.college.placement.modules.student.repository.SkillRepository;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.outbox.repository.OutboxEventRepository;
import com.college.placement.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full Spring application context loads and key beans are wired
 * correctly with the test (H2) profile.
 */
@SpringBootTest(classes = com.college.placement.Application.class)
@ActiveProfiles("test")
class ApplicationStartupTest {

    @Autowired ApplicationContext context;
    @Autowired AppUserRepository appUserRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired RecruiterRepository recruiterRepository;
    @Autowired JobPostingRepository jobPostingRepository;
    @Autowired CertificateRepository certificateRepository;
    @Autowired JobApplicationRepository jobApplicationRepository;
    @Autowired OfferRepository offerRepository;
    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired JwtService jwtService;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void allJpaRepositoriesLoad() {
        assertThat(appUserRepository).isNotNull();
        assertThat(refreshTokenRepository).isNotNull();
        assertThat(studentRepository).isNotNull();
        assertThat(branchRepository).isNotNull();
        assertThat(skillRepository).isNotNull();
        assertThat(companyRepository).isNotNull();
        assertThat(recruiterRepository).isNotNull();
        assertThat(jobPostingRepository).isNotNull();
        assertThat(certificateRepository).isNotNull();
        assertThat(jobApplicationRepository).isNotNull();
        assertThat(offerRepository).isNotNull();
        assertThat(outboxEventRepository).isNotNull();
    }

    @Test
    void securityFilterChainBeanIsPresent() {
        assertThat(context.getBean(SecurityFilterChain.class)).isNotNull();
    }

    @Test
    void jwtServiceBeanIsPresent() {
        assertThat(jwtService).isNotNull();
    }

    @Test
    void outboxRepositorySupportsAllStatusQueries() {
        // Verifies the custom JPQL queries in OutboxEventRepository compile and execute without throwing
        long pending = outboxEventRepository.countByStatus(
                com.college.placement.shared.outbox.domain.OutboxStatus.PENDING);
        assertThat(pending).isNotNegative();

        org.springframework.data.domain.Page<?> dead = outboxEventRepository.findDeadLetterEvents(
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(dead).isNotNull();
    }
}
