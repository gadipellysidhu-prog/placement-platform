-- =====================================================================
-- V17_0_0__verification_tokens.sql
-- Generic single-use verification tokens (email verification, password
-- reset, invitations, MFA, email change). Only the SHA-256 hash of the raw
-- token is stored; the raw value is never persisted.
-- =====================================================================

CREATE TABLE IF NOT EXISTS verification_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed    BOOLEAN      NOT NULL DEFAULT FALSE,
    consumed_at TIMESTAMPTZ,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  VARCHAR(255),
    metadata    TEXT,
    CONSTRAINT uq_verification_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id)
        REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_user_type
    ON verification_tokens (user_id, type);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_expires_at
    ON verification_tokens (expires_at);
