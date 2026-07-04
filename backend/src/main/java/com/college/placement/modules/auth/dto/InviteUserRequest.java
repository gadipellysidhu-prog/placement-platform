package com.college.placement.modules.auth.dto;

import com.college.placement.modules.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Invite a new privileged user")
public record InviteUserRequest(
        @NotBlank @Email String email,
        @NotNull Role role
) {}
