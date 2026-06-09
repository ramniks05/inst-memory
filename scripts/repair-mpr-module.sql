-- Run once against PostgreSQL (e.g. dolr_inst on the server).
-- Idempotent: safe to re-run. Creates the MPR module when Flyway is disabled on deploy.
-- Requires existing table: users (uploaded_by_user_id FK).
--
-- Equivalent to Flyway: V10__mpr_module.sql + V11__mpr_numeric_period.sql

BEGIN;

CREATE TABLE IF NOT EXISTS mprs (
    id                   BIGSERIAL PRIMARY KEY,
    uploaded_by_user_id  BIGINT        NOT NULL REFERENCES users (id),
    division_name        VARCHAR(255)  NOT NULL,
    subject              VARCHAR(500)  NOT NULL,
    report_type          VARCHAR(20)   NOT NULL,
    financial_year       VARCHAR(10)   NOT NULL,
    period_label         VARCHAR(50),
    original_file_name   VARCHAR(255)  NOT NULL,
    stored_file_name     VARCHAR(255)  NOT NULL,
    stored_relative_path VARCHAR(1024) NOT NULL,
    file_size            BIGINT        NOT NULL,
    upload_date          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    financial_year_start SMALLINT,
    period_value         SMALLINT
);

ALTER TABLE mprs
    ADD COLUMN IF NOT EXISTS financial_year_start SMALLINT,
    ADD COLUMN IF NOT EXISTS period_value         SMALLINT;

UPDATE mprs SET financial_year_start = CAST(SPLIT_PART(financial_year, '-', 1) AS SMALLINT)
WHERE financial_year_start IS NULL AND financial_year ~ '^[0-9]{4}-';

UPDATE mprs SET period_value = CASE period_label
    WHEN 'April'     THEN 4  WHEN 'May'       THEN 5
    WHEN 'June'      THEN 6  WHEN 'July'      THEN 7
    WHEN 'August'    THEN 8  WHEN 'September' THEN 9
    WHEN 'October'   THEN 10 WHEN 'November'  THEN 11
    WHEN 'December'  THEN 12 WHEN 'January'   THEN 1
    WHEN 'February'  THEN 2  WHEN 'March'     THEN 3
    ELSE NULL END
WHERE report_type = 'MONTHLY' AND period_value IS NULL;

UPDATE mprs SET period_value = CASE
    WHEN period_label LIKE 'Q1%' THEN 1
    WHEN period_label LIKE 'Q2%' THEN 2
    WHEN period_label LIKE 'Q3%' THEN 3
    WHEN period_label LIKE 'Q4%' THEN 4
    ELSE NULL END
WHERE report_type = 'QUARTERLY' AND period_value IS NULL;

CREATE INDEX IF NOT EXISTS idx_mprs_upload_date ON mprs (upload_date DESC);
CREATE INDEX IF NOT EXISTS idx_mprs_uploaded_by ON mprs (uploaded_by_user_id);
CREATE INDEX IF NOT EXISTS idx_mprs_report_type_fy ON mprs (report_type, financial_year);
CREATE INDEX IF NOT EXISTS idx_mprs_fy_type_period ON mprs (financial_year_start, report_type, period_value);
CREATE INDEX IF NOT EXISTS idx_mprs_division ON mprs (division_name);

COMMIT;
