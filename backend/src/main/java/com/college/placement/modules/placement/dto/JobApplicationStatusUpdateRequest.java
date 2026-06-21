package com.college.placement.modules.placement.dto;

import com.college.placement.modules.placement.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update a job application's status")
public record JobApplicationStatusUpdateRequest(

        @NotNull
        @Schema(description = "New application status")
        ApplicationStatus status
) {}
