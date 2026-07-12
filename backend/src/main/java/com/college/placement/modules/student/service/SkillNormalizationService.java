package com.college.placement.modules.student.service;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.repository.SkillAliasRepository;
import com.college.placement.modules.student.repository.SkillRepository;
import com.college.placement.modules.student.service.matching.SkillSimilarityMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Skill Normalization Engine — resolves a raw skill string ("ReactJS", "CPP",
 * " machine learning ") to its canonical catalog {@link Skill}, or reports that no
 * match exists so the caller can decide whether to create one.
 *
 * Resolution chain (first hit wins):
 *   1. exact canonical name, case/whitespace-insensitive
 *   2. alias lookup on the normalized form
 *   3. static abbreviation expansion (fallback for aliases the catalog lacks)
 *   4. fuzzy similarity via {@link SkillSimilarityMatcher} (spelling mistakes)
 */
@Slf4j
@Service
public class SkillNormalizationService {

    /**
     * Fallback abbreviation → canonical-name expansions. The alias table is the primary
     * mechanism (it grows continuously); this map only guarantees the most common
     * abbreviations resolve even on a catalog whose aliases were pruned.
     */
    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("cpp", "C++"),
            Map.entry("c plus plus", "C++"),
            Map.entry("csharp", "C#"),
            Map.entry("js", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("py", "Python"),
            Map.entry("golang", "Go"),
            Map.entry("ml", "Machine Learning"),
            Map.entry("dl", "Deep Learning"),
            Map.entry("ai", "Artificial Intelligence"),
            Map.entry("nlp", "Natural Language Processing"),
            Map.entry("cv", "Computer Vision"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("aws", "Amazon Web Services"),
            Map.entry("gcp", "Google Cloud Platform"),
            Map.entry("db", "Databases"),
            Map.entry("oop", "Object-Oriented Programming"),
            Map.entry("dsa", "Data Structures"),
            Map.entry("spring", "Spring Framework")
    );

    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;
    private final SkillSimilarityMatcher similarityMatcher;

    public SkillNormalizationService(SkillRepository skillRepository,
                                     SkillAliasRepository skillAliasRepository,
                                     SkillSimilarityMatcher similarityMatcher) {
        this.skillRepository = skillRepository;
        this.skillAliasRepository = skillAliasRepository;
        this.similarityMatcher = similarityMatcher;
    }

    /** Canonical normalization: trim, collapse internal whitespace, lower-case. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Resolve a raw skill string to its canonical catalog skill, if one exists. */
    @Transactional(readOnly = true)
    public Optional<Skill> resolve(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty() || normalized.length() > 100) {
            return Optional.empty();
        }

        // 1. Exact canonical name (case/whitespace-insensitive).
        Optional<Skill> byName = skillRepository.findByNameIgnoreCase(normalized);
        if (byName.isPresent()) {
            return activeOnly(byName);
        }

        // 2. Alias database.
        Optional<Skill> byAlias = skillAliasRepository.findByAliasNormalized(normalized)
                .map(a -> a.getSkill());
        if (byAlias.isPresent()) {
            return activeOnly(byAlias);
        }

        // 3. Abbreviation expansion, then re-check name + alias for the expansion.
        String expansion = ABBREVIATIONS.get(normalized);
        if (expansion != null) {
            Optional<Skill> byExpansion = skillRepository.findByNameIgnoreCase(expansion)
                    .or(() -> skillAliasRepository.findByAliasNormalized(normalize(expansion))
                            .map(a -> a.getSkill()));
            if (byExpansion.isPresent()) {
                return activeOnly(byExpansion);
            }
        }

        // 4. Fuzzy match (spelling mistakes: "Javva", "Kubernets").
        List<SkillSimilarityMatcher.SimilarityHit> hits = similarityMatcher.topMatches(normalized, 1);
        if (!hits.isEmpty()) {
            Optional<Skill> fuzzy = skillRepository.findById(hits.get(0).skillId());
            if (fuzzy.isPresent()) {
                log.debug("SKILL_NORMALIZE event=FUZZY_MATCH raw={} matched={} score={}",
                        normalized, fuzzy.get().getName(), hits.get(0).score());
                return activeOnly(fuzzy);
            }
        }
        return Optional.empty();
    }

    /**
     * Normalize a batch of raw skill strings: dedupes input (two raw forms resolving to
     * the same skill count once) and collects the unresolved leftovers.
     */
    @Transactional(readOnly = true)
    public NormalizationResult normalizeAll(List<String> rawSkills) {
        Map<java.util.UUID, Skill> resolved = new LinkedHashMap<>();
        List<String> unresolved = new ArrayList<>();
        for (String raw : rawSkills) {
            String normalized = normalize(raw);
            if (normalized.isEmpty()) {
                continue;
            }
            resolve(raw).ifPresentOrElse(
                    skill -> resolved.putIfAbsent(skill.getId(), skill),
                    () -> {
                        if (!unresolved.contains(normalized)) {
                            unresolved.add(raw.trim().replaceAll("\\s+", " "));
                        }
                    });
        }
        return new NormalizationResult(List.copyOf(resolved.values()), List.copyOf(unresolved));
    }

    private Optional<Skill> activeOnly(Optional<Skill> skill) {
        return skill.filter(Skill::isActive);
    }

    /** Outcome of a batch normalization: canonical skills plus the raw strings with no match. */
    public record NormalizationResult(List<Skill> resolved, List<String> unresolved) {}
}
