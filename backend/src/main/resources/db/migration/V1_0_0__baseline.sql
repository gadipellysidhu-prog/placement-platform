-- =====================================================================
-- V1_0_0__baseline.sql
-- Baseline migration for the Placement Intelligence & Skill Verification
-- Platform. Enables required PostgreSQL extensions and records platform
-- metadata. Idempotent per MOP 10.2 (Phase 3) -- safe to re-run.
--
-- Domain tables (users, students, companies, applications, outbox_events,
-- file_scan_records, ai_models, ...) are introduced in later increments,
-- each as its own versioned migration.
-- =====================================================================

-- pg_trgm powers trigram text search on skill names (SADD 13.3 / 6).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Simple key/value table used to assert connectivity and schema version.
CREATE TABLE IF NOT EXISTS app_metadata (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO app_metadata (key, value) VALUES
    ('schema_version', '1.0.0'),
    ('platform_name',  'Placement Intelligence & Skill Verification Platform')
ON CONFLICT (key) DO NOTHING;
