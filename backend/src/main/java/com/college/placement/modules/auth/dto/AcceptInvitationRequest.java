package com.college.placement.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Accept an invitation and set the account password")
public record AcceptInvitationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
