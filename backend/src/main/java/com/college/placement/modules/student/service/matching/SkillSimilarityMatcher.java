package com.college.placement.modules.student.service.matching;

import java.util.List;
import java.util.UUID;

/**
 * Fuzzy-matching port for the skills catalog. The production implementation uses
 * PostgreSQL pg_trgm (gin indexes on skills.name / skill_aliases.alias exist since
 * V1_0_0/V19_0_0); the test-profile implementation is a dialect-free in-memory
 * matcher, because the H2 test database cannot execute trigram SQL.
 */
public interface SkillSimilarityMatcher {

    /**
     * Best fuzzy matches for a normalized (lower/trim/space-collapsed) query, across
     * both canonical names and aliases, scored 0..1 and ordered best-first. Only
     * matches at or above the implementation's similarity threshold are returned.
     */
    List<SimilarityHit> topMatches(String normalizedQuery, int limit);

    /** A fuzzy match candidate: the skill id and a 0..1 similarity score. */
    record SimilarityHit(UUID skillId, double score) {}
}
