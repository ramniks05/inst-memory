-- Designations: some roles apply to every division (e.g. Secretary); others are tied to one division.

CREATE TABLE IF NOT EXISTS designations (
    id                       BIGSERIAL PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    handles_all_divisions    BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order               INTEGER NOT NULL DEFAULT 0,
    active                   BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT designations_name_unique UNIQUE (name)
);

INSERT INTO designations (name, handles_all_divisions, sort_order, active) VALUES
    ('Secretary', TRUE, 10, TRUE),
    ('Under Secretary', FALSE, 20, TRUE),
    ('Section Officer', FALSE, 30, TRUE)
ON CONFLICT (name) DO NOTHING;

ALTER TABLE users ADD COLUMN IF NOT EXISTS division_fk_id BIGINT REFERENCES divisions (id);

ALTER TABLE users ADD COLUMN IF NOT EXISTS designation_fk_id BIGINT REFERENCES designations (id);
