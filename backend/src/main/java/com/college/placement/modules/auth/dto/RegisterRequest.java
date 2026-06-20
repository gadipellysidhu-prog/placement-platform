package com.college.placement.modules.auth.dto;

import com.college.placement.modules.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 8, max = 128)
    String password,

    @NotNull
    Role role
) {}
