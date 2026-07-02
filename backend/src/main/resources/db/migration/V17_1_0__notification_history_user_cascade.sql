-- =====================================================================
-- V17_1_0__notification_history_user_cascade.sql
-- Align notification_history with every other user-owned table by cascading
-- on user deletion. Previously the only user FK without ON DELETE CASCADE,
-- it blocked account removal once delivery history existed.
-- =====================================================================

ALTER TABLE notification_history
    DROP CONSTRAINT IF EXISTS notification_history_user_id_fkey;

ALTER TABLE notification_history
    ADD CONSTRAINT fk_notification_history_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE;
