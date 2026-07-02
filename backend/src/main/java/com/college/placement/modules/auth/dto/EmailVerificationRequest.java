package com.college.placement.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request to (re)send an email-verification link. */
public record EmailVerificationRequest(
        @NotBlank @Email String email
) {}
