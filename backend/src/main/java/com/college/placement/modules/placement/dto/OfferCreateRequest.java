package com.college.placement.modules.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request to create a placement offer")
public record OfferCreateRequest(

        @NotNull
        @Schema(description = "Job application UUID (must be in OFFERED status)")
        UUID applicationId,

        @DecimalMin("0.0")
        @Schema(description = "Cost to Company in LPA", example = "12.5")
        BigDecimal ctc,

        @Schema(description = "Expected joining date")
        LocalDate joiningDate
) {}
