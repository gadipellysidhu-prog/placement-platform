package com.college.placement.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

    @NotBlank
    String refreshToken
) {}
