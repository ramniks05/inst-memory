-- Master list of departmental divisions (admin-managed).

CREATE TABLE IF NOT EXISTS divisions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT divisions_name_unique UNIQUE (name)
);

INSERT INTO divisions (name, sort_order, active) VALUES
    ('LR Division', 10, true),
    ('Watershed Division', 20, true)
ON CONFLICT (name) DO NOTHING;
