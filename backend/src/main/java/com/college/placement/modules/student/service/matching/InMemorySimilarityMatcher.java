package com.college.placement.modules.student.service.matching;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.repository.SkillAliasRepository;
import com.college.placement.modules.student.repository.SkillRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Dialect-free fuzzy matcher for the H2 test profile (H2 has no pg_trgm). Loads
 * active names + aliases and scores them with normalized Levenshtein similarity.
 * Catalogs in tests are tiny, so a full scan is fine; production uses
 * {@link PgTrgmSimilarityMatcher}.
 */
@Component
@Profile("test")
public class InMemorySimilarityMatcher implements SkillSimilarityMatcher {

    /** Aligned with the trigram threshold so tests exercise equivalent behaviour. */
    static final double THRESHOLD = 0.45;

    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;

    public InMemorySimilarityMatcher(SkillRepository skillRepository,
                                     SkillAliasRepository skillAliasRepository) {
        this.skillRepository = skillRepository;
        this.skillAliasRepository = skillAliasRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimilarityHit> topMatches(String normalizedQuery, int limit) {
        Map<UUID, Double> best = new LinkedHashMap<>();

        for (Skill skill : skillRepository.findByActiveTrue()) {
            double score = similarity(normalizedQuery, skill.getName().toLowerCase(Locale.ROOT));
            if (score >= THRESHOLD) {
                best.merge(skill.getId(), score, Math::max);
            }
        }
        skillAliasRepository.findAll().forEach(alias -> {
            double score = similarity(normalizedQuery, alias.getAliasNormalized());
            if (score >= THRESHOLD) {
                best.merge(alias.getSkill().getId(), score, Math::max);
            }
        });

        List<SimilarityHit> hits = new ArrayList<>();
        best.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .forEach(e -> hits.add(new SimilarityHit(e.getKey(), e.getValue())));
        return hits;
    }

    /** Normalized Levenshtein similarity: 1 - distance / max(len). */
    public static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - (double) levenshtein(a, b) / maxLen;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
