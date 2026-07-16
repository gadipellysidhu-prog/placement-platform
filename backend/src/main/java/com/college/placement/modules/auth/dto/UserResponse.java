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
        Instant updatedAt,
        @Schema(description = "Most recent login/refresh, derived from refresh-token issuance. "
                + "Null when no activity is on record — never inferred from createdAt/updatedAt.")
        Instant lastActivityAt
) {
    /** Without activity context — {@code lastActivityAt} is reported as unknown. */
    public static UserResponse from(AppUser u) {
        return from(u, null);
    }

    public static UserResponse from(AppUser u, Instant lastActivityAt) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getRole(), u.getStatus(),
                u.isEmailVerified(), u.getCreatedAt(), u.getUpdatedAt(), lastActivityAt);
    }
}
