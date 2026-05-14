-- Accounts linked to a division or designation are departmental users, not portal administrators.
-- Normalise mistaken ADMIN role so uploads and officer UI match roster intent.

UPDATE users
SET role = 'OFFICER',
    is_officer = true
WHERE (designation_fk_id IS NOT NULL OR division_fk_id IS NOT NULL)
  AND UPPER(TRIM(COALESCE(role, ''))) = 'ADMIN';
