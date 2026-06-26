package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a skill")
public record SkillCreateRequest(

        @NotBlank @Size(max = 100)
        @Schema(description = "Skill name", example = "Java")
        String name,

        @Size(max = 50)
        @Schema(description = "Category", example = "Programming")
        String category
) {}
