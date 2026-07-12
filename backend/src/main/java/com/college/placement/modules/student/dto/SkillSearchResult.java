package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.service.SkillService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ranked skill search result")
public record SkillSearchResult(
        UUID id,
        String name,
        String category,
        String parentCategory,
        int popularityScore,
        @Schema(description = "How the query matched: EXACT, ALIAS, PARTIAL or FUZZY")
        String matchType,
        @Schema(description = "Ranking score 0..1 (higher ranks first)")
        double score
) {
    public static SkillSearchResult from(SkillService.SkillSearchHit hit) {
        var s = hit.skill();
        return new SkillSearchResult(s.getId(), s.getName(), s.getCategory(),
                s.getParentCategory(), s.getPopularityScore(), hit.matchType(), hit.score());
    }
}
