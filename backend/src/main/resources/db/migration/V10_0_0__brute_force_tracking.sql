-- =====================================================================
-- V10_0_0__brute_force_tracking.sql
-- Add login-attempt tracking columns to app_users for brute-force
-- protection.  No new table — reuses the existing auth identity record.
-- =====================================================================

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS failure_count   INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_failure_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS locked_until    TIMESTAMPTZ;

COMMENT ON COLUMN app_users.failure_count   IS 'Consecutive failed login attempts since last success';
COMMENT ON COLUMN app_users.last_failure_at IS 'Timestamp of the most recent failed login attempt';
COMMENT ON COLUMN app_users.locked_until    IS 'Temporary lockout expiry; NULL means not locked';
