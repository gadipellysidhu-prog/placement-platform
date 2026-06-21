package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Schema(description = "Request to create a student profile")
public record StudentCreateRequest(

        @NotNull
        @Schema(description = "UUID of the AppUser to link this student profile to")
        UUID userId,

        @NotBlank
        @Size(max = 50)
        @Schema(description = "Unique roll number", example = "CS2021001")
        String rollNumber,

        @Schema(description = "Branch UUID (optional)")
        UUID branchId,

        @Min(1)
        @Max(6)
        @Schema(description = "Current academic year (1–6)", example = "2")
        int currentYear
) {}
