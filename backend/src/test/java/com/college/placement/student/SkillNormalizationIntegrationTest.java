package com.college.placement.student;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.SkillAlias;
import com.college.placement.modules.student.domain.SkillCreatedSource;
import com.college.placement.modules.student.repository.SkillAliasRepository;
import com.college.placement.modules.student.repository.SkillRepository;
import com.college.placement.modules.student.service.SkillNormalizationService;
import com.college.placement.modules.student.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.college.placement.Application.class)
@ActiveProfiles("test")
class SkillNormalizationIntegrationTest {

    @Autowired SkillNormalizationService normalizationService;
    @Autowired SkillService skillService;
    @Autowired SkillRepository skillRepository;
    @Autowired SkillAliasRepository skillAliasRepository;

    @BeforeEach
    void clean() {
        skillAliasRepository.deleteAll();
        skillRepository.deleteAll();
    }

    private Skill seed(String name, String category, String... aliases) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(category);
        skill.setCreatedSource(SkillCreatedSource.SEED);
        skill = skillRepository.save(skill);
        for (String alias : aliases) {
            SkillAlias a = new SkillAlias();
            a.setSkill(skill);
            a.setAlias(alias);
            a.setAliasNormalized(SkillNormalizationService.normalize(alias));
            skillAliasRepository.save(a);
        }
        return skill;
    }

    @Test
    void resolvesExactNameCaseAndWhitespaceInsensitive() {
        Skill java = seed("Java", "Programming Languages");

        assertThat(normalizationService.resolve("  jAvA  ")).contains(java);
        assertThat(normalizationService.resolve("Java")).contains(java);
    }

    @Test
    void resolvesAliasesToCanonicalSkill() {
        Skill react = seed("React", "Web Frontend", "ReactJS", "React.js", "React JS");

        assertThat(normalizationService.resolve("ReactJS")).contains(react);
        assertThat(normalizationService.resolve("react.js")).contains(react);
        assertThat(normalizationService.resolve("REACT   JS")).contains(react);
    }

    @Test
    void resolvesAbbreviationsViaStaticExpansion() {
        Skill cpp = seed("C++", "Programming Languages");
        Skill ml = seed("Machine Learning", "AI & Machine Learning");

        assertThat(normalizationService.resolve("CPP")).contains(cpp);
        assertThat(normalizationService.resolve("ml")).contains(ml);
    }

    @Test
    void resolvesSpellingMistakesViaFuzzyMatch() {
        Skill kubernetes = seed("Kubernetes", "DevOps");

        assertThat(normalizationService.resolve("Kubernets")).contains(kubernetes);
        assertThat(normalizationService.resolve("Kuberneties")).contains(kubernetes);
    }

    @Test
    void unknownSkillResolvesEmpty() {
        seed("Java", "Programming Languages");

        assertThat(normalizationService.resolve("Quantum Basket Weaving")).isEmpty();
        assertThat(normalizationService.resolve("")).isEmpty();
        assertThat(normalizationService.resolve("   ")).isEmpty();
    }

    @Test
    void inactiveSkillsAreNeverResolved() {
        Skill legacy = seed("Flash", "Web Frontend");
        legacy.setActive(false);
        skillRepository.save(legacy);

        assertThat(normalizationService.resolve("Flash")).isEmpty();
    }

    @Test
    void normalizeAllDedupesInputsResolvingToTheSameSkill() {
        seed("React", "Web Frontend", "ReactJS");

        var result = normalizationService.normalizeAll(
                List.of("React", "ReactJS", "react", "Cobolt Fusion X"));

        assertThat(result.resolved()).hasSize(1);
        assertThat(result.resolved().get(0).getName()).isEqualTo("React");
        assertThat(result.unresolved()).containsExactly("Cobolt Fusion X");
    }

    @Test
    void findOrCreateReusesExistingSkillThroughEveryResolutionPath() {
        Skill react = seed("React", "Web Frontend", "ReactJS");

        var byName = skillService.findOrCreate("react", null, SkillCreatedSource.AI, null);
        var byAlias = skillService.findOrCreate("ReactJS", null, SkillCreatedSource.AI, null);

        assertThat(byName.created()).isFalse();
        assertThat(byAlias.created()).isFalse();
        assertThat(byName.skill()).isEqualTo(react);
        assertThat(byAlias.skill()).isEqualTo(react);
        assertThat(skillRepository.count()).isEqualTo(1);
    }

    @Test
    void findOrCreateCreatesNewSkillWithSourceAndAutoAliases() {
        var result = skillService.findOrCreate("Node.js", "Backend Frameworks",
                SkillCreatedSource.AI, new java.math.BigDecimal("87.50"));

        assertThat(result.created()).isTrue();
        Skill created = result.skill();
        assertThat(created.getName()).isEqualTo("Node.js");
        assertThat(created.getCreatedSource()).isEqualTo(SkillCreatedSource.AI);
        assertThat(created.getAiConfidence()).isEqualByComparingTo("87.50");

        // Auto-generated variants make "nodejs" resolvable without fuzzy matching.
        Optional<Skill> resolved = normalizationService.resolve("nodejs");
        assertThat(resolved).contains(created);

        // And a repeated discovery reuses instead of duplicating.
        var again = skillService.findOrCreate("NodeJS", null, SkillCreatedSource.AI, null);
        assertThat(again.created()).isFalse();
        assertThat(skillRepository.count()).isEqualTo(1);
    }
}
