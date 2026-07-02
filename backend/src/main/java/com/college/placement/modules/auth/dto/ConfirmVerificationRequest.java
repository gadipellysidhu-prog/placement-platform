package com.college.placement.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirms an email-verification token. */
public record ConfirmVerificationRequest(
        @NotBlank String token
) {}
