package com.college.placement.repository;

import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.CompanyStatus;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.shared.audit.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CompanyRepositoryTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired TestEntityManager em;

    // ── findByName ────────────────────────────────────────────────────────────

    @Test
    void findByName_existingCompany_returnsCompany() {
        em.persistAndFlush(buildCompany("TechCorp", "Technology"));

        Optional<Company> result = companyRepository.findByName("TechCorp");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("TechCorp");
        assertThat(result.get().getIndustry()).isEqualTo("Technology");
    }

    @Test
    void findByName_nonExisting_returnsEmpty() {
        Optional<Company> result = companyRepository.findByName("NoSuchCorp");

        assertThat(result).isEmpty();
    }

    // ── existsByName ──────────────────────────────────────────────────────────

    @Test
    void existsByName_whenPresent_returnsTrue() {
        em.persistAndFlush(buildCompany("AcmeCo", "Finance"));

        assertThat(companyRepository.existsByName("AcmeCo")).isTrue();
    }

    @Test
    void existsByName_whenAbsent_returnsFalse() {
        assertThat(companyRepository.existsByName("Ghost Inc")).isFalse();
    }

    // ── findByIndustry ────────────────────────────────────────────────────────

    @Test
    void findByIndustry_multipleCompanies_returnsAll() {
        em.persistAndFlush(buildCompany("Alpha Tech", "Technology"));
        em.persistAndFlush(buildCompany("Beta Tech", "Technology"));
        em.persistAndFlush(buildCompany("Gamma Finance", "Finance"));

        List<Company> techCompanies = companyRepository.findByIndustry("Technology");

        assertThat(techCompanies).hasSize(2)
                .extracting(Company::getName)
                .containsExactlyInAnyOrder("Alpha Tech", "Beta Tech");
    }

    // ── unique constraint ─────────────────────────────────────────────────────

    @Test
    void save_duplicateName_throwsConstraintViolation() {
        em.persistAndFlush(buildCompany("DupCorp", "Tech"));

        Company duplicate = buildCompany("DupCorp", "Finance");

        assertThatThrownBy(() -> companyRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── default status ────────────────────────────────────────────────────────

    @Test
    void save_newCompany_defaultStatusIsActive() {
        Company saved = companyRepository.saveAndFlush(buildCompany("StatusCo", "Retail"));

        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    // ── auditing fields ───────────────────────────────────────────────────────

    @Test
    void save_newCompany_auditingFieldsPopulated() {
        Company saved = companyRepository.saveAndFlush(buildCompany("AuditCo", "IT"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Company buildCompany(String name, String industry) {
        Company c = new Company();
        c.setName(name);
        c.setIndustry(industry);
        return c;
    }
}
