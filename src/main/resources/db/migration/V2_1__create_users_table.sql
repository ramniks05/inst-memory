-- Application users (matches JPA User entity; Spring Boot snake_case column names).
-- Must run before V3 (ALTER users) and V4 (documents FK to users).

CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    full_name               VARCHAR(255),
    email                   VARCHAR(255) CONSTRAINT uk_users_email UNIQUE,
    mobile_number           VARCHAR(255),
    department              VARCHAR(255),
    division                VARCHAR(255),
    designation             VARCHAR(255),
    password                VARCHAR(255),
    role                    VARCHAR(32),
    approved                BOOLEAN,
    is_officer              BOOLEAN,
    reporting_officer_id    BIGINT REFERENCES users (id)
);
