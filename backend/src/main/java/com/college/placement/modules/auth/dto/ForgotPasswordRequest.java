package com.college.placement.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request to send a password-reset link. */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {}
