-- V14_0_0__fix_students_current_year_type.sql
-- Corrects the column type mismatch found during production Hibernate schema validation.
-- V2_0_0 created current_year as SMALLINT; the Student entity maps it as Integer (INTEGER).
-- ALTER COLUMN ... TYPE is a metadata-only operation in PostgreSQL when widening a numeric type.
ALTER TABLE students ALTER COLUMN current_year TYPE INTEGER;
