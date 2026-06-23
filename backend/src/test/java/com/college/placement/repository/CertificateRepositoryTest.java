package com.college.placement.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.certificate.domain.Certificate;
import com.college.placement.modules.certificate.domain.CertificateVerificationStatus;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.shared.audit.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CertificateRepositoryTest {

    @Autowired CertificateRepository certificateRepository;
    @Autowired TestEntityManager em;

    private Student student;

    @BeforeEach
    void setUp() {
        AppUser user = em.persistAndFlush(buildUser("cert-student@test.com"));
        student = em.persistAndFlush(buildStudent(user, "CS2021099"));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existingCertificate_returnsCertificate() {
        Certificate saved = em.persistAndFlush(buildCertificate(student, "AWS Certified", null));

        Optional<Certificate> result = certificateRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("AWS Certified");
        assertThat(result.get().getVerificationStatus()).isEqualTo(CertificateVerificationStatus.PENDING);
    }

    // ── findByStudent ─────────────────────────────────────────────────────────

    @Test
    void findByStudent_returnsCertificatesForStudent() {
        em.persistAndFlush(buildCertificate(student, "Oracle Certified", null));
        em.persistAndFlush(buildCertificate(student, "GCP Associate", null));

        List<Certificate> certs = certificateRepository.findByStudent(student);

        assertThat(certs).hasSize(2)
                .extracting(Certificate::getName)
                .containsExactlyInAnyOrder("Oracle Certified", "GCP Associate");
    }

    @Test
    void findByStudent_noCertificates_returnsEmptyList() {
        AppUser other = em.persistAndFlush(buildUser("other@test.com"));
        Student otherStudent = em.persistAndFlush(buildStudent(other, "CS2021098"));

        List<Certificate> certs = certificateRepository.findByStudent(otherStudent);

        assertThat(certs).isEmpty();
    }

    // ── findByStudentAndVerificationStatus ────────────────────────────────────

    @Test
    void findByStudentAndVerificationStatus_pending_returnsPendingOnly() {
        em.persistAndFlush(buildCertificate(student, "Pending Cert", null));
        Certificate verified = buildCertificate(student, "Verified Cert", null);
        verified.setVerificationStatus(CertificateVerificationStatus.VERIFIED);
        em.persistAndFlush(verified);

        List<Certificate> pending = certificateRepository.findByStudentAndVerificationStatus(
                student, CertificateVerificationStatus.PENDING);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getName()).isEqualTo("Pending Cert");
    }

    @Test
    void findByStudentAndVerificationStatus_verified_returnsVerifiedOnly() {
        em.persistAndFlush(buildCertificate(student, "Pending Cert", null));
        Certificate verified = buildCertificate(student, "Verified Cert", null);
        verified.setVerificationStatus(CertificateVerificationStatus.VERIFIED);
        em.persistAndFlush(verified);

        List<Certificate> verifiedCerts = certificateRepository.findByStudentAndVerificationStatus(
                student, CertificateVerificationStatus.VERIFIED);

        assertThat(verifiedCerts).hasSize(1);
        assertThat(verifiedCerts.get(0).getName()).isEqualTo("Verified Cert");
    }

    // ── findAll pageable ──────────────────────────────────────────────────────

    @Test
    void findAll_pageable_returnsPaginatedResults() {
        em.persistAndFlush(buildCertificate(student, "Cert A", null));
        em.persistAndFlush(buildCertificate(student, "Cert B", null));
        em.persistAndFlush(buildCertificate(student, "Cert C", null));

        Page<Certificate> page = certificateRepository.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // ── with skill ────────────────────────────────────────────────────────────

    @Test
    void findById_certificateWithSkill_loadsSkill() {
        Skill skill = new Skill();
        skill.setName("Java");
        skill.setCategory("Programming");
        Skill savedSkill = em.persistAndFlush(skill);

        Certificate cert = buildCertificate(student, "Java Certified", savedSkill);
        Certificate saved = em.persistAndFlush(cert);

        Optional<Certificate> result = certificateRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getSkill()).isNotNull();
        assertThat(result.get().getSkill().getName()).isEqualTo("Java");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static AppUser buildUser(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$hash");
        u.setRole(Role.ROLE_STUDENT);
        return u;
    }

    private static Student buildStudent(AppUser user, String rollNumber) {
        Student s = new Student();
        s.setUser(user);
        s.setRollNumber(rollNumber);
        s.setCurrentYear(2);
        s.setStatus(StudentStatus.ACTIVE);
        return s;
    }

    private static Certificate buildCertificate(Student student, String name, Skill skill) {
        Certificate c = new Certificate();
        c.setStudent(student);
        c.setName(name);
        c.setIssuingOrganization("Test Org");
        c.setSkill(skill);
        return c;
    }
}
