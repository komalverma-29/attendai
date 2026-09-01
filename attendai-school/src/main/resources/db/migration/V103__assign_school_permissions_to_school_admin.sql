-- V103: Assign all SCHOOL_* permissions to the SCHOOL_ADMIN role
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.module = 'SCHOOL'
  AND p.is_deleted = FALSE;
