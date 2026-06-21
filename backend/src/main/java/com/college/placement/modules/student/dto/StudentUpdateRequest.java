package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request to update a student profile")
public record StudentUpdateRequest(

        @Schema(description = "Branch UUID (optional)")
        UUID branchId,

        @DecimalMin("0.0")
        @DecimalMax("10.0")
        @Schema(description = "CGPA on a 10-point scale", example = "8.5")
        BigDecimal cgpa,

        @Min(1)
        @Max(6)
        @Schema(description = "Current academic year (1–6)", example = "3")
        int currentYear
) {}
