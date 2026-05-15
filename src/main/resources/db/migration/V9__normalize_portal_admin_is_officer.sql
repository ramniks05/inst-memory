-- Legacy ADMIN rows with is_officer NULL were excluded from the admin menu on the server.
UPDATE users
SET is_officer = false
WHERE UPPER(TRIM(COALESCE(role, ''))) = 'ADMIN'
  AND is_officer IS NULL;
