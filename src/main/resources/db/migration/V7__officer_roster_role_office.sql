-- Roster officers must carry OFFICER role so the portal never treats them as administrators.
-- Fixes rows where is_officer = true but role was left as ADMIN (or any non-officer code).

UPDATE users
SET role = 'OFFICER'
WHERE is_officer = true
  AND UPPER(TRIM(COALESCE(role, ''))) <> 'OFFICER';
