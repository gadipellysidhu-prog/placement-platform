package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.domain.Skill;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Skill response")
public record SkillResponse(
        UUID id,
        String name,
        String category,
        boolean verified,
        Instant createdAt,
        Instant updatedAt
) {
    public static SkillResponse from(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getCategory(),
                s.isVerified(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
