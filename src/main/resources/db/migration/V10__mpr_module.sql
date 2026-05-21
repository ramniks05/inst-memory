-- MPR (Monthly Progress Report) uploads by officers

CREATE TABLE IF NOT EXISTS mprs (
    id                   BIGSERIAL PRIMARY KEY,
    uploaded_by_user_id  BIGINT        NOT NULL REFERENCES users (id),
    division_name        VARCHAR(255)  NOT NULL,
    subject              VARCHAR(500)  NOT NULL,
    report_type          VARCHAR(20)   NOT NULL,   -- MONTHLY | QUARTERLY | YEARLY
    financial_year       VARCHAR(10)   NOT NULL,   -- e.g. 2024-25
    period_label         VARCHAR(50),              -- e.g. April | Q1 (Apr-Jun) | null for yearly
    original_file_name   VARCHAR(255)  NOT NULL,
    stored_file_name     VARCHAR(255)  NOT NULL,
    stored_relative_path VARCHAR(1024) NOT NULL,
    file_size            BIGINT        NOT NULL,
    upload_date          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mprs_upload_date        ON mprs (upload_date DESC);
CREATE INDEX idx_mprs_uploaded_by        ON mprs (uploaded_by_user_id);
CREATE INDEX idx_mprs_report_type_fy     ON mprs (report_type, financial_year);
