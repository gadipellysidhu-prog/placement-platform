package com.college.placement.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request to update a job posting (only allowed in DRAFT status)")
public record JobPostingUpdateRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Job title")
        String title,

        @Schema(description = "Job description")
        String description,

        @DecimalMin("0.0")
        @Schema(description = "Minimum CTC")
        BigDecimal ctcMin,

        @DecimalMin("0.0")
        @Schema(description = "Maximum CTC")
        BigDecimal ctcMax,

        @Schema(description = "Application deadline")
        LocalDate applicationDeadline,

        @Min(1)
        @Schema(description = "Offer limit")
        int offerLimit
) {}
