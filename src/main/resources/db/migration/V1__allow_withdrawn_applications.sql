DO $$
BEGIN
    IF to_regclass('job_applications') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE job_applications DROP CONSTRAINT IF EXISTS job_applications_status_check';
        EXECUTE 'ALTER TABLE job_applications ADD CONSTRAINT job_applications_status_check '
             || 'CHECK (status IN (''SUBMITTED'', ''REVIEWING'', ''INTERVIEW'', ''REJECTED'', ''HIRED'', ''WITHDRAWN''))';
    END IF;
END
$$;
