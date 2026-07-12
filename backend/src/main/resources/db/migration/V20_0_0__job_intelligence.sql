-- Job Intelligence module: AI extraction pipeline run tracking + per-URL extraction cache.
-- Purely additive — no existing table or relationship changes.

CREATE TABLE job_intelligence_runs (
    id                 UUID PRIMARY KEY,
    version            BIGINT        NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    job_posting_id     UUID          NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    official_url       VARCHAR(2048) NOT NULL,
    url_hash           VARCHAR(64)   NOT NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    provider           VARCHAR(100),
    model              VARCHAR(100),
    confidence         NUMERIC(5,2),
    skills_extracted   INT           NOT NULL DEFAULT 0,
    skills_created     INT           NOT NULL DEFAULT 0,
    skills_tagged      INT           NOT NULL DEFAULT 0,
    predicted_branches VARCHAR(500),
    extracted_json     TEXT,
    warnings_json      TEXT,
    error_message      TEXT,
    requested_by       VARCHAR(255),
    retry_count        INT           NOT NULL DEFAULT 0,
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    duration_ms        BIGINT
);

CREATE INDEX idx_job_intel_runs_posting ON job_intelligence_runs (job_posting_id, created_at DESC);
CREATE INDEX idx_job_intel_runs_status  ON job_intelligence_runs (status);

-- Cache of validated structured AI output per URL so re-processing the same
-- official job URL reuses the previous extraction instead of re-crawling and
-- re-invoking the LLM (unless expired or explicitly refreshed).
CREATE TABLE job_extraction_cache (
    id              UUID PRIMARY KEY,
    version         BIGINT        NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    url_hash        VARCHAR(64)   NOT NULL,
    url             VARCHAR(2048) NOT NULL,
    structured_json TEXT          NOT NULL,
    provider        VARCHAR(100),
    model           VARCHAR(100),
    expires_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_job_extraction_cache_url_hash UNIQUE (url_hash)
);

CREATE INDEX idx_job_extraction_cache_expires ON job_extraction_cache (expires_at);
