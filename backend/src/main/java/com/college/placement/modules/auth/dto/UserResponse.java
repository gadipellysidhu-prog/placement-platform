package com.college.placement.modules.auth.dto;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Administrative view of a user account")
public record UserResponse(
        UUID id,
        String email,
        Role role,
        AccountStatus status,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getRole(), u.getStatus(),
                u.isEmailVerified(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
