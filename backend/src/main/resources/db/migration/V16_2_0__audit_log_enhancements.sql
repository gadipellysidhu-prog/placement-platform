-- =====================================================================
-- V16_2_0__audit_log_enhancements.sql
-- Enrich audit_logs with correlation / request metadata and before/after
-- values so every privileged or mutating action is fully reconstructable.
-- All columns are additive and nullable (success defaults TRUE) so existing
-- audit writes (e.g. DomainEventAuditHandler) continue to work unchanged.
-- =====================================================================

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ip_address     VARCHAR(64),
    ADD COLUMN IF NOT EXISTS user_agent     VARCHAR(512),
    ADD COLUMN IF NOT EXISTS previous_value TEXT,
    ADD COLUMN IF NOT EXISTS new_value      TEXT,
    ADD COLUMN IF NOT EXISTS reason         VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS success        BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation ON audit_logs (correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at  ON audit_logs (created_at);
