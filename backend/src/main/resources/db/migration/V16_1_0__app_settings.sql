-- =====================================================================
-- V16_1_0__app_settings.sql
-- Configuration-driven settings store. Backs OTP TTLs, policy defaults,
-- AI / storage / notification provider toggles, etc. Settings may be
-- global (academic_year_id NULL) or scoped to a specific academic year.
-- =====================================================================

CREATE TABLE IF NOT EXISTS app_settings (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    setting_key      VARCHAR(150) NOT NULL,
    setting_value    TEXT,
    value_type       VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    category         VARCHAR(80),
    description      VARCHAR(500),
    academic_year_id UUID,
    CONSTRAINT fk_app_settings_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_years (id) ON DELETE CASCADE
);

-- Global settings (no academic year) are unique by key.
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_settings_global_key
    ON app_settings (setting_key) WHERE academic_year_id IS NULL;

-- Year-scoped settings are unique by (key, year).
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_settings_year_key
    ON app_settings (setting_key, academic_year_id) WHERE academic_year_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_app_settings_category ON app_settings (category);
