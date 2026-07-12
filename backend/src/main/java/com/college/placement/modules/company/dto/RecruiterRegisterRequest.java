package com.college.placement.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to register a recruiter profile against a company")
public record RecruiterRegisterRequest(

        @NotNull
        @Schema(description = "UUID of the AppUser to link this recruiter profile to")
        UUID userId,

        @NotNull
        @Schema(description = "UUID of the company the recruiter belongs to")
        UUID companyId,

        @Size(max = 100)
        @Schema(description = "Recruiter designation (optional)", example = "Talent Acquisition Lead")
        String designation
) {}
