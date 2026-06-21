package com.college.placement.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update a company's details")
public record CompanyUpdateRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Company name")
        String name,

        @Size(max = 255)
        @Schema(description = "Website URL")
        String website,

        @Size(max = 100)
        @Schema(description = "Industry sector")
        String industry,

        @Schema(description = "Company description")
        String description,

        @Size(max = 500)
        @Schema(description = "Logo URL")
        String logoUrl
) {}
