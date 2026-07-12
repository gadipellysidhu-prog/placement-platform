package com.college.placement.modules.jobintelligence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to start an AI extraction run for a job posting")
public record StartRunRequest(

        @NotNull
        @Schema(description = "Draft job posting to analyze")
        UUID jobPostingId,

        @NotBlank
        @Size(max = 2048)
        @Schema(description = "Official job posting URL (http/https)",
                example = "https://careers.example.com/jobs/backend-engineer")
        String officialUrl
) {}
