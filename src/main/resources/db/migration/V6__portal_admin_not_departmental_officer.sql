-- Portal administrators must not be flagged as departmental roster officers.
-- Fixes legacy/bootstrap rows with role ADMIN and is_officer true, which incorrectly showed the admin menu.

UPDATE users
SET is_officer = false
WHERE UPPER(TRIM(COALESCE(role, ''))) = 'ADMIN';
