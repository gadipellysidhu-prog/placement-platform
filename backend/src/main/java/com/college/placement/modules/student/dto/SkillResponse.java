package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.SkillAlias;
import com.college.placement.modules.student.domain.SkillCreatedSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Skill response")
public record SkillResponse(
        UUID id,
        String name,
        String category,
        boolean verified,
        String description,
        String parentCategory,
        String subcategory,
        int popularityScore,
        String industryTags,
        boolean active,
        SkillCreatedSource createdSource,
        BigDecimal aiConfidence,
        @Schema(description = "Populated on detail responses; null on list responses")
        List<String> aliases,
        Instant createdAt,
        Instant updatedAt
) {
    /** List/summary projection — aliases not loaded. */
    public static SkillResponse from(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getCategory(), s.isVerified(),
                s.getDescription(), s.getParentCategory(), s.getSubcategory(),
                s.getPopularityScore(), s.getIndustryTags(), s.isActive(),
                s.getCreatedSource(), s.getAiConfidence(), null,
                s.getCreatedAt(), s.getUpdatedAt());
    }

    /** Detail projection including the skill's aliases. */
    public static SkillResponse fromDetailed(Skill s, List<SkillAlias> aliases) {
        return new SkillResponse(s.getId(), s.getName(), s.getCategory(), s.isVerified(),
                s.getDescription(), s.getParentCategory(), s.getSubcategory(),
                s.getPopularityScore(), s.getIndustryTags(), s.isActive(),
                s.getCreatedSource(), s.getAiConfidence(),
                aliases.stream().map(SkillAlias::getAlias).toList(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
