package com.college.placement.modules.auth.dto;

import com.college.placement.modules.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Assign a role to a user")
public record AssignRoleRequest(
        @NotNull Role role
) {}
