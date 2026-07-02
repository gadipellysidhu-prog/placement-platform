-- Add updated_at column required by the Auditable base class.
ALTER TABLE application_status_history
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
