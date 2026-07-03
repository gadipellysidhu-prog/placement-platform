-- =====================================================================
-- V18_0_0__user_account_status.sql
-- Administrative account lifecycle state for IAM (Phase D). Existing users
-- default to ACTIVE; invited users are created as INVITED and activated on
-- acceptance. Distinct from the transient brute-force lock (locked_until).
-- =====================================================================

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_app_users_status ON app_users (status);
CREATE INDEX IF NOT EXISTS idx_app_users_role   ON app_users (role);
