package com.college.placement.student;

import com.college.placement.modules.student.service.matching.InMemorySimilarityMatcher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for the test-profile Levenshtein similarity scoring. */
class InMemorySimilarityMatcherTest {

    @Test
    void identicalStringsScoreOne() {
        assertThat(InMemorySimilarityMatcher.similarity("java", "java")).isEqualTo(1.0);
    }

    @Test
    void closeMisspellingsScoreAboveThreshold() {
        assertThat(InMemorySimilarityMatcher.similarity("kubernets", "kubernetes")).isGreaterThan(0.45);
        assertThat(InMemorySimilarityMatcher.similarity("javva", "java")).isGreaterThan(0.45);
        assertThat(InMemorySimilarityMatcher.similarity("pythn", "python")).isGreaterThan(0.45);
    }

    @Test
    void unrelatedStringsScoreBelowThreshold() {
        assertThat(InMemorySimilarityMatcher.similarity("leadership", "kubernetes")).isLessThan(0.45);
        assertThat(InMemorySimilarityMatcher.similarity("a", "completely different")).isLessThan(0.45);
    }

    @Test
    void similarityIsSymmetric() {
        double ab = InMemorySimilarityMatcher.similarity("react", "redux");
        double ba = InMemorySimilarityMatcher.similarity("redux", "react");
        assertThat(ab).isEqualTo(ba);
    }
}
