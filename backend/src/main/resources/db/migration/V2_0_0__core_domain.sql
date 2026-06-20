-- =====================================================================
-- V2_0_0__core_domain.sql
-- Core domain tables: student, company, placement, certificate,
-- notification, audit, outbox, file pipeline, AI governance.
-- =====================================================================

-- ── Branches ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS branches (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(20),
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_branches_name UNIQUE (name),
    CONSTRAINT uq_branches_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_branches_name ON branches (name);

-- ── Skills ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS skills (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(50),
    verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_skills_name UNIQUE (name)
);
CREATE INDEX IF NOT EXISTS idx_skills_name_trgm ON skills USING gin (name gin_trgm_ops);

-- ── Students ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS students (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    version            BIGINT      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id            UUID        NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    branch_id          UUID        REFERENCES branches (id),
    roll_number        VARCHAR(50) NOT NULL,
    cgpa               NUMERIC(4,2),
    current_year       SMALLINT    NOT NULL DEFAULT 1,
    placement_eligible BOOLEAN     NOT NULL DEFAULT TRUE,
    status             VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_students_user_id      UNIQUE (user_id),
    CONSTRAINT uq_students_roll_number  UNIQUE (roll_number)
);
CREATE INDEX IF NOT EXISTS idx_students_branch_id ON students (branch_id);
CREATE INDEX IF NOT EXISTS idx_students_status    ON students (status);

-- ── Student ↔ Skill (M:M) ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS student_skills (
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    skill_id   UUID NOT NULL REFERENCES skills   (id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, skill_id)
);

-- ── Companies ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS companies (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    name        VARCHAR(255) NOT NULL,
    website     VARCHAR(255),
    industry    VARCHAR(100),
    description TEXT,
    logo_url    VARCHAR(500),
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_companies_name UNIQUE (name)
);
CREATE INDEX IF NOT EXISTS idx_companies_industry ON companies (industry);
CREATE INDEX IF NOT EXISTS idx_companies_status   ON companies (status);

-- ── Recruiters ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS recruiters (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    user_id     UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    company_id  UUID         NOT NULL REFERENCES companies (id),
    designation VARCHAR(100),
    CONSTRAINT uq_recruiters_user_id UNIQUE (user_id)
);
CREATE INDEX IF NOT EXISTS idx_recruiters_company_id ON recruiters (company_id);

-- ── Job Postings ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS job_postings (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    company_id           UUID        NOT NULL REFERENCES companies (id),
    title                VARCHAR(255) NOT NULL,
    description          TEXT,
    ctc_min              NUMERIC(15,2),
    ctc_max              NUMERIC(15,2),
    status               VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    application_deadline DATE,
    offer_limit          INT         NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_job_postings_company_id ON job_postings (company_id);
CREATE INDEX IF NOT EXISTS idx_job_postings_status     ON job_postings (status);

-- ── Job Posting ↔ Skill (M:M) ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS job_posting_skills (
    job_posting_id UUID NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    skill_id       UUID NOT NULL REFERENCES skills       (id),
    PRIMARY KEY (job_posting_id, skill_id)
);

-- ── Job Posting ↔ Branch (eligible branches, M:M) ────────────────────

CREATE TABLE IF NOT EXISTS job_posting_branches (
    job_posting_id UUID NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    branch_id      UUID NOT NULL REFERENCES branches     (id),
    PRIMARY KEY (job_posting_id, branch_id)
);

-- ── Job Applications ──────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS job_applications (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    student_id      UUID        NOT NULL REFERENCES students     (id),
    job_posting_id  UUID        NOT NULL REFERENCES job_postings (id),
    status          VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_job_applications_student_posting UNIQUE (student_id, job_posting_id)
);
CREATE INDEX IF NOT EXISTS idx_job_applications_student_id     ON job_applications (student_id);
CREATE INDEX IF NOT EXISTS idx_job_applications_job_posting_id ON job_applications (job_posting_id);
CREATE INDEX IF NOT EXISTS idx_job_applications_status         ON job_applications (status);

-- ── Offers ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS offers (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    application_id  UUID         NOT NULL REFERENCES job_applications (id),
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    ctc             NUMERIC(15,2),
    joining_date    DATE,
    CONSTRAINT uq_offers_application_id UNIQUE (application_id)
);

-- ── Certificates ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS certificates (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    student_id           UUID         NOT NULL REFERENCES students (id),
    skill_id             UUID         REFERENCES skills (id),
    name                 VARCHAR(255) NOT NULL,
    issuing_organization VARCHAR(255),
    file_key             VARCHAR(500),
    verification_status  VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
);
CREATE INDEX IF NOT EXISTS idx_certificates_student_id ON certificates (student_id);

-- ── Notification History ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notification_history (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    user_id     UUID         NOT NULL REFERENCES app_users (id),
    channel     VARCHAR(50)  NOT NULL,
    subject     VARCHAR(500) NOT NULL,
    body        TEXT         NOT NULL,
    sent_at     TIMESTAMPTZ,
    status      VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
);
CREATE INDEX IF NOT EXISTS idx_notification_history_user_id ON notification_history (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_history_status  ON notification_history (status);

-- ── Audit Logs ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     VARCHAR(100) NOT NULL,
    action        VARCHAR(100) NOT NULL,
    performed_by  VARCHAR(255),
    payload       TEXT
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity       ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_performed_by ON audit_logs (performed_by);

-- ── Outbox Events ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    processed_at    TIMESTAMPTZ,
    error_message   TEXT
);
CREATE INDEX IF NOT EXISTS idx_outbox_events_status        ON outbox_events (status);
CREATE INDEX IF NOT EXISTS idx_outbox_events_next_retry_at ON outbox_events (next_retry_at)
    WHERE status = 'PENDING';

-- ── File Scan Records ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS file_scan_records (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    filename      VARCHAR(500) NOT NULL,
    storage_key   VARCHAR(500) NOT NULL,
    content_type  VARCHAR(255),
    size_bytes    BIGINT,
    sha256_hash   VARCHAR(64),
    scan_status   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    quarantined   BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by   VARCHAR(255),
    CONSTRAINT uq_file_scan_records_storage_key UNIQUE (storage_key)
);
CREATE INDEX IF NOT EXISTS idx_file_scan_records_scan_status ON file_scan_records (scan_status);
CREATE INDEX IF NOT EXISTS idx_file_scan_records_sha256      ON file_scan_records (sha256_hash);

-- ── AI Models ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS ai_models (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    name          VARCHAR(100) NOT NULL,
    provider      VARCHAR(100) NOT NULL,
    model_version VARCHAR(100),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    config_json   TEXT,
    CONSTRAINT uq_ai_models_name UNIQUE (name)
);

-- ── Prompt Registry ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS prompt_registry (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ai_model_id    UUID         NOT NULL REFERENCES ai_models (id),
    prompt_key     VARCHAR(100) NOT NULL,
    template       TEXT         NOT NULL,
    prompt_version INT          NOT NULL DEFAULT 1,
    CONSTRAINT uq_prompt_registry_key UNIQUE (prompt_key)
);
CREATE INDEX IF NOT EXISTS idx_prompt_registry_ai_model_id ON prompt_registry (ai_model_id);

-- ── Inference History ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS inference_history (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ai_model_id        UUID         NOT NULL REFERENCES ai_models      (id),
    prompt_registry_id UUID         REFERENCES prompt_registry (id),
    input_tokens       INT          NOT NULL DEFAULT 0,
    output_tokens      INT          NOT NULL DEFAULT 0,
    latency_ms         BIGINT       NOT NULL DEFAULT 0,
    requested_by       VARCHAR(255),
    success            BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message      TEXT
);
CREATE INDEX IF NOT EXISTS idx_inference_history_ai_model_id  ON inference_history (ai_model_id);
CREATE INDEX IF NOT EXISTS idx_inference_history_requested_by ON inference_history (requested_by);

-- ── Schema version update ─────────────────────────────────────────────

INSERT INTO app_metadata (key, value) VALUES ('schema_version', '2.0.0')
    ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = NOW();
