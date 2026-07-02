package com.college.placement.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update a branch")
public record BranchUpdateRequest(

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 20)
        String code,

        String description
) {}
