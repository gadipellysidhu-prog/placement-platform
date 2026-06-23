package com.college.placement.repository;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.repository.SkillRepository;
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
class SkillRepositoryTest {

    @Autowired SkillRepository skillRepository;
    @Autowired TestEntityManager em;

    // ── findByName ────────────────────────────────────────────────────────────

    @Test
    void findByName_existingSkill_returnsSkill() {
        em.persistAndFlush(buildSkill("Java", "Programming"));

        Optional<Skill> result = skillRepository.findByName("Java");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Java");
        assertThat(result.get().getCategory()).isEqualTo("Programming");
    }

    @Test
    void findByName_nonExisting_returnsEmpty() {
        assertThat(skillRepository.findByName("COBOL")).isEmpty();
    }

    // ── existsByName ──────────────────────────────────────────────────────────

    @Test
    void existsByName_whenPresent_returnsTrue() {
        em.persistAndFlush(buildSkill("Python", "Programming"));

        assertThat(skillRepository.existsByName("Python")).isTrue();
    }

    @Test
    void existsByName_whenAbsent_returnsFalse() {
        assertThat(skillRepository.existsByName("Fortran")).isFalse();
    }

    // ── findByCategory ────────────────────────────────────────────────────────

    @Test
    void findByCategory_multipleSkills_returnsAll() {
        em.persistAndFlush(buildSkill("React", "Frontend"));
        em.persistAndFlush(buildSkill("Vue.js", "Frontend"));
        em.persistAndFlush(buildSkill("Spring Boot", "Backend"));

        List<Skill> frontendSkills = skillRepository.findByCategory("Frontend");

        assertThat(frontendSkills).hasSize(2)
                .extracting(Skill::getName)
                .containsExactlyInAnyOrder("React", "Vue.js");
    }

    // ── findByVerifiedTrue ────────────────────────────────────────────────────

    @Test
    void findByVerifiedTrue_returnsOnlyVerifiedSkills() {
        Skill verified = buildSkill("AWS", "Cloud");
        verified.setVerified(true);
        em.persistAndFlush(verified);
        em.persistAndFlush(buildSkill("Azure", "Cloud")); // unverified

        List<Skill> verifiedSkills = skillRepository.findByVerifiedTrue();

        assertThat(verifiedSkills).hasSize(1);
        assertThat(verifiedSkills.get(0).getName()).isEqualTo("AWS");
    }

    // ── unique constraint ─────────────────────────────────────────────────────

    @Test
    void save_duplicateName_throwsConstraintViolation() {
        em.persistAndFlush(buildSkill("Docker", "DevOps"));

        assertThatThrownBy(() -> skillRepository.saveAndFlush(buildSkill("Docker", "Containers")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── default verified flag ─────────────────────────────────────────────────

    @Test
    void save_newSkill_defaultVerifiedIsFalse() {
        Skill saved = skillRepository.saveAndFlush(buildSkill("Kubernetes", "DevOps"));

        assertThat(saved.isVerified()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Skill buildSkill(String name, String category) {
        Skill s = new Skill();
        s.setName(name);
        s.setCategory(category);
        return s;
    }
}
