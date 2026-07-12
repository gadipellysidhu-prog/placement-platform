package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to add an alias to a skill")
public record SkillAliasRequest(

        @NotBlank @Size(max = 100)
        @Schema(description = "Alternative name or abbreviation", example = "ReactJS")
        String alias
) {}
