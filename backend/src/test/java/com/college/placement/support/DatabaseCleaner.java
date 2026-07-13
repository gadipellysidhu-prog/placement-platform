package com.college.placement.support;

import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.repository.RecruiterRepository;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared test cleanup that removes the AppUser-linked entity graph in foreign-key-safe
 * order: offers → applications → certificates → students → refresh tokens → app users.
 *
 * <p>Integration tests share one in-memory H2 database within a Surefire JVM (the Spring
 * context — and therefore the datasource — is cached across test classes). Because most
 * tests clean in {@code @BeforeEach}, the data created by a class's <em>last</em> test
 * survives into the next class. A class that then deletes {@code app_users} without first
 * clearing the {@code students} (and their applications/offers/certificates) that
 * reference them hits a referential-integrity violation.
 *
 * <p>Calling {@link #clean()} at the start of such a class's cleanup guarantees a clean
 * slate regardless of Surefire run order. {@code deleteAll()} (not {@code deleteAllInBatch})
 * is used so Hibernate also clears the {@code student_skills} join table.
 */
@Component
public class DatabaseCleaner {

    private final OfferRepository offerRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CertificateRepository certificateRepository;
    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;

    public DatabaseCleaner(OfferRepository offerRepository,
                           JobApplicationRepository jobApplicationRepository,
                           CertificateRepository certificateRepository,
                           StudentRepository studentRepository,
                           RecruiterRepository recruiterRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           AppUserRepository appUserRepository) {
        this.offerRepository = offerRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.certificateRepository = certificateRepository;
        this.studentRepository = studentRepository;
        this.recruiterRepository = recruiterRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Deletes every AppUser-linked entity in FK-safe order (children before parents).
     * In the Hibernate-generated H2 test schema, students, recruiters and refresh tokens
     * reference app_users without ON DELETE CASCADE (unlike verification tokens and
     * notifications, which cascade via @OnDelete), so each must be cleared explicitly.
     */
    @Transactional
    public void clean() {
        offerRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        certificateRepository.deleteAll();
        studentRepository.deleteAll();
        recruiterRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        appUserRepository.deleteAll();
    }
}
