package com.college.placement.modules.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to submit a job application")
public record JobApplicationCreateRequest(

        @NotNull
        @Schema(description = "Student UUID")
        UUID studentId,

        @NotNull
        @Schema(description = "Job posting UUID")
        UUID jobPostingId
) {}
