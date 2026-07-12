package com.college.placement.modules.student.service.matching;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * pg_trgm-backed fuzzy matcher. Runs a native similarity() query against both
 * canonical names and aliases, served by the gin trigram indexes. The SQL is built
 * via EntityManager at call time (never through a Spring Data interface) so the H2
 * test profile never parses PostgreSQL-specific syntax.
 */
@Component
@Profile("!test")
public class PgTrgmSimilarityMatcher implements SkillSimilarityMatcher {

    /** Trigram similarity floor — below this, matches are noise ("Java" vs "JavaScript" ≈ 0.36). */
    static final double THRESHOLD = 0.45;

    private static final String SQL = """
            SELECT id, score FROM (
                SELECT s.id AS id, similarity(lower(s.name), :q) AS score
                FROM skills s
                WHERE s.active = TRUE AND similarity(lower(s.name), :q) >= :threshold
                UNION ALL
                SELECT a.skill_id AS id, similarity(lower(a.alias), :q) AS score
                FROM skill_aliases a
                JOIN skills s2 ON s2.id = a.skill_id
                WHERE s2.active = TRUE AND similarity(lower(a.alias), :q) >= :threshold
            ) hits
            ORDER BY score DESC
            """;

    private final EntityManager entityManager;

    public PgTrgmSimilarityMatcher(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimilarityHit> topMatches(String normalizedQuery, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(SQL)
                .setParameter("q", normalizedQuery)
                .setParameter("threshold", THRESHOLD)
                .setMaxResults(limit * 4) // room for name+alias duplicates before dedupe
                .getResultList();

        // Deduplicate by skill id keeping the best score, preserve score ordering.
        Map<UUID, Double> best = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UUID id = (UUID) row[0];
            double score = ((Number) row[1]).doubleValue();
            best.merge(id, score, Math::max);
        }
        List<SimilarityHit> hits = new ArrayList<>();
        best.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .forEach(e -> hits.add(new SimilarityHit(e.getKey(), e.getValue())));
        return hits;
    }
}
