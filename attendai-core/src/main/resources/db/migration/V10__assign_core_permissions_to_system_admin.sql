-- V10: Assign all Core permissions to SYSTEM_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SYSTEM_ADMIN'
  AND p.module = 'CORE'
  AND p.is_deleted = FALSE;
