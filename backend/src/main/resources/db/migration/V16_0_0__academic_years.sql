-- =====================================================================
-- V16_0_0__academic_years.sql
-- Academic year / placement season reference data.
-- Referenced by placement policy versioning, analytics snapshots and
-- recruitment seasons (later increments). Exactly one row may be active.
-- =====================================================================

CREATE TABLE IF NOT EXISTS academic_years (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    label       VARCHAR(20) NOT NULL,   -- e.g. 2025-2026
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_academic_years_label UNIQUE (label),
    CONSTRAINT ck_academic_years_dates CHECK (end_date > start_date)
);

-- At most one active academic year at any time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_academic_years_single_active
    ON academic_years (active) WHERE active = TRUE;
