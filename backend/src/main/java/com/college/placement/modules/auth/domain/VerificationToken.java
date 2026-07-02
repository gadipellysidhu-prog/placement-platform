package com.college.placement.modules.auth.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * A single-use, time-limited verification token bound to a user and a purpose
 * ({@link VerificationTokenType}). Only the SHA-256 hash of the raw token is
 * stored — the raw value exists only in transit (e.g. in a verification email)
 * and is never persisted.
 *
 * <p>Lifecycle: a token is valid while {@code !consumed && !revoked && now < expiresAt}.
 * Consumption is one-time (replay protection); issuing a new token of the same
 * type for a user revokes the previous active ones (only the latest is valid).
 */
@Entity
@Table(
    name = "verification_tokens",
    indexes = {
        @Index(name = "idx_verification_tokens_user_type",  columnList = "user_id, type"),
        @Index(name = "idx_verification_tokens_expires_at",  columnList = "expires_at"),
        @Index(name = "idx_verification_tokens_token_hash",  columnList = "token_hash", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
public class VerificationToken extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owning user. Loaded lazily; {@code VerificationTokenService.consume} initialises it within its transaction. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    /** SHA-256 hex of the raw token. Never store the raw value. */
    @NotBlank
    @Size(max = 64)
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationTokenType type;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed = false;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(nullable = false)
    private boolean revoked = false;

    /** Optional actor that initiated issuance (e.g. the admin who sent an invitation). */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /** Optional opaque metadata (e.g. JSON) for workflow-specific context. */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VerificationToken t)) return false;
        return id != null && id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
