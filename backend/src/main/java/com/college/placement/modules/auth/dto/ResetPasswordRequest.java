package com.college.placement.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Confirms a password reset with a token and the new password. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
