package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request to approve a pending student registration. The target {@code AppUser} is
 * identified by the path variable; this body carries the profile details the officer
 * assigns at approval time. Mirrors the profile fields of {@link StudentCreateRequest}
 * (minus {@code userId}) so both entry points feed the same creation logic.
 */
@Schema(description = "Details assigned when approving a pending student registration")
public record StudentApprovalRequest(

        @NotBlank
        @Size(max = 50)
        @Schema(description = "Unique roll number to assign", example = "CS2021001")
        String rollNumber,

        @Schema(description = "Branch UUID (optional)")
        UUID branchId,

        @Min(1)
        @Max(6)
        @Schema(description = "Current academic year (1–6)", example = "1")
        int currentYear
) {}
