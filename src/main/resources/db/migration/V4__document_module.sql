-- Document types (admin-managed): SOM, reports, meetings, etc.

CREATE TABLE IF NOT EXISTS document_types (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT document_types_name_unique UNIQUE (name)
);

INSERT INTO document_types (name, sort_order, active) VALUES
    ('SOM', 10, TRUE),
    ('Report', 20, TRUE),
    ('Meeting minutes', 30, TRUE),
    ('Circular', 40, TRUE),
    ('Other', 100, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Replace legacy documents table (previous ad-hoc schema) with portal documents + designation visibility.

DROP TABLE IF EXISTS document_visible_designations CASCADE;
DROP TABLE IF EXISTS documents CASCADE;

CREATE TABLE documents (
    id                     BIGSERIAL PRIMARY KEY,
    title                  VARCHAR(500) NOT NULL,
    document_type_id       BIGINT NOT NULL REFERENCES document_types (id),
    uploaded_by_user_id    BIGINT NOT NULL REFERENCES users (id),
    original_file_name     VARCHAR(255) NOT NULL,
    stored_file_name       VARCHAR(255) NOT NULL,
    stored_relative_path   VARCHAR(1024) NOT NULL,
    file_size              BIGINT NOT NULL,
    upload_date            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_visible_designations (
    document_id     BIGINT NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    designation_id  BIGINT NOT NULL REFERENCES designations (id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, designation_id)
);

CREATE INDEX idx_documents_upload_date ON documents (upload_date DESC);
