-- Application status history: records every status transition for an application.
-- This table is append-only; rows are never updated.
CREATE TABLE application_status_history (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID         NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    previous_status VARCHAR(50),
    new_status     VARCHAR(50)  NOT NULL,
    changed_by     VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_app_status_history_application_id
    ON application_status_history (application_id);
