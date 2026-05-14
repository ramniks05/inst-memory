-- Reference table for application roles (users.role stores the code).
-- Used for documentation, reporting, and future FK constraints if desired.

CREATE TABLE IF NOT EXISTS roles (
    code        VARCHAR(32) PRIMARY KEY,
    label       VARCHAR(160) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

INSERT INTO roles (code, label, sort_order) VALUES
    ('ADMIN',       'Administrator',        10),
    ('OFFICER',     'Departmental officer', 20)
ON CONFLICT (code) DO NOTHING;
