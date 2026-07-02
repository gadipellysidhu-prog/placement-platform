package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a branch")
public record BranchCreateRequest(

        @NotBlank @Size(max = 100)
        @Schema(description = "Branch name", example = "Computer Science")
        String name,

        @Size(max = 20)
        @Schema(description = "Short code", example = "CS")
        String code,

        @Schema(description = "Description")
        String description
) {}
