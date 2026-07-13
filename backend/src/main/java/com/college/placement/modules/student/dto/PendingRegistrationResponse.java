package com.college.placement.modules.student.dto;

import com.college.placement.modules.auth.domain.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered student account that is awaiting approval — i.e. an {@code AppUser}
 * with {@code ROLE_STUDENT} that has no linked {@link com.college.placement.modules.student.domain.Student}
 * profile yet. There is no separate "registration request" entity: the absence of a
 * profile <em>is</em> the pending state, and creating the profile is the approval.
 *
 * <p>{@code AppUser} carries no name field, so {@link #displayName} is derived from the
 * email local part purely for presentation.
 */
@Schema(description = "A student registration awaiting officer approval")
public record PendingRegistrationResponse(
        UUID userId,
        String email,
        String displayName,
        boolean emailVerified,
        Instant createdAt
) {
    public static PendingRegistrationResponse from(AppUser user) {
        return new PendingRegistrationResponse(
                user.getId(),
                user.getEmail(),
                deriveDisplayName(user.getEmail()),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    /** Best-effort human name from the email local part (e.g. "john.doe" → "John Doe"). */
    private static String deriveDisplayName(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String[] parts = local.split("[._-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.length() > 0 ? sb.toString() : local;
    }
}
