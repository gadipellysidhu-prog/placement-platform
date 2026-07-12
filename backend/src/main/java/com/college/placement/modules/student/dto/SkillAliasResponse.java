package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.domain.SkillAlias;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Skill alias response")
public record SkillAliasResponse(
        UUID id,
        UUID skillId,
        String alias
) {
    public static SkillAliasResponse from(SkillAlias a) {
        return new SkillAliasResponse(a.getId(), a.getSkill().getId(), a.getAlias());
    }
}
