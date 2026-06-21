package com.college.placement.modules.student.dto;

import com.college.placement.modules.student.domain.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update a student's placement status")
public record StudentStatusUpdateRequest(

        @NotNull
        @Schema(description = "New student status")
        StudentStatus status
) {}
