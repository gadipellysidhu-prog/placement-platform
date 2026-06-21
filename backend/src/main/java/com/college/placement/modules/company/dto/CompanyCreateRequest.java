package com.college.placement.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new company")
public record CompanyCreateRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Company name", example = "Acme Corp")
        String name,

        @Size(max = 255)
        @Schema(description = "Company website URL")
        String website,

        @Size(max = 100)
        @Schema(description = "Industry sector", example = "Technology")
        String industry,

        @Schema(description = "Company description")
        String description
) {}
