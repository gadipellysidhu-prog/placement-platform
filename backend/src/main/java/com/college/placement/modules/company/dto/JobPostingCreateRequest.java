package com.college.placement.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request to create a job posting")
public record JobPostingCreateRequest(

        @NotNull
        @Schema(description = "Company UUID")
        UUID companyId,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Job title", example = "Backend Engineer")
        String title,

        @Schema(description = "Job description")
        String description,

        @DecimalMin("0.0")
        @Schema(description = "Minimum CTC in LPA")
        BigDecimal ctcMin,

        @DecimalMin("0.0")
        @Schema(description = "Maximum CTC in LPA")
        BigDecimal ctcMax,

        @Schema(description = "Application deadline")
        LocalDate applicationDeadline,

        @Min(1)
        @Schema(description = "Maximum number of offers", example = "5")
        int offerLimit
) {}
