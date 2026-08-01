-- V9: Seed Core system permissions
INSERT INTO permissions (code, name, module, description, is_system, is_deleted) VALUES
-- User management
('CORE_USER_CREATE', 'Create User',   'CORE', 'Create a new user account',  TRUE, FALSE),
('CORE_USER_READ',   'Read User',     'CORE', 'Read user details',          TRUE, FALSE),
('CORE_USER_UPDATE', 'Update User',   'CORE', 'Update a user account',      TRUE, FALSE),
('CORE_USER_DELETE', 'Delete User',   'CORE', 'Delete a user account',      TRUE, FALSE),
-- Role management
('CORE_ROLE_CREATE', 'Create Role',   'CORE', 'Create a new role',          TRUE, FALSE),
('CORE_ROLE_READ',   'Read Role',     'CORE', 'Read role details',          TRUE, FALSE),
('CORE_ROLE_UPDATE', 'Update Role',   'CORE', 'Update a role',              TRUE, FALSE),
('CORE_ROLE_DELETE', 'Delete Role',   'CORE', 'Delete a role',              TRUE, FALSE),
('CORE_ROLE_ASSIGN', 'Assign Role',   'CORE', 'Assign/remove roles to users', TRUE, FALSE),
-- Permission management
('CORE_PERMISSION_CREATE', 'Create Permission', 'CORE', 'Create a new permission',           TRUE, FALSE),
('CORE_PERMISSION_READ',   'Read Permission',   'CORE', 'Read permission details',           TRUE, FALSE),
('CORE_PERMISSION_UPDATE', 'Update Permission', 'CORE', 'Update a permission',               TRUE, FALSE),
('CORE_PERMISSION_DELETE', 'Delete Permission', 'CORE', 'Delete a permission',               TRUE, FALSE),
('CORE_PERMISSION_ASSIGN', 'Assign Permission', 'CORE', 'Assign/remove permissions to roles', TRUE, FALSE),
-- Person management
('CORE_PERSON_CREATE', 'Create Person', 'CORE', 'Create a person record', TRUE, FALSE),
('CORE_PERSON_READ',   'Read Person',   'CORE', 'Read person details',    TRUE, FALSE),
('CORE_PERSON_UPDATE', 'Update Person', 'CORE', 'Update a person record', TRUE, FALSE),
('CORE_PERSON_DELETE', 'Delete Person', 'CORE', 'Delete a person record', TRUE, FALSE),
-- Audit
('CORE_AUDIT_READ',   'Read Audit Logs',    'CORE', 'Read audit logs',           TRUE, FALSE),
-- Config
('CORE_CONFIG_READ',  'Read Config',        'CORE', 'Read system configuration', TRUE, FALSE),
('CORE_CONFIG_WRITE', 'Write Config',       'CORE', 'Write system configuration', TRUE, FALSE),
-- Station
('CORE_STATION_CREATE', 'Create Station', 'CORE', 'Register a new station',  TRUE, FALSE),
('CORE_STATION_READ',   'Read Station',   'CORE', 'Read station details',    TRUE, FALSE),
('CORE_STATION_UPDATE', 'Update Station', 'CORE', 'Update a station',        TRUE, FALSE),
('CORE_STATION_DELETE', 'Delete Station', 'CORE', 'Delete a station',        TRUE, FALSE),
-- Notification
('CORE_NOTIFICATION_READ',   'Read Notifications',   'CORE', 'Read notifications',            TRUE, FALSE),
('CORE_NOTIFICATION_MANAGE', 'Manage Notifications', 'CORE', 'Manage notification templates', TRUE, FALSE),
-- File
('CORE_FILE_UPLOAD', 'Upload File', 'CORE', 'Upload a file',         TRUE, FALSE),
('CORE_FILE_READ',   'Read File',   'CORE', 'Read/download a file',  TRUE, FALSE),
('CORE_FILE_DELETE', 'Delete File', 'CORE', 'Delete a file',         TRUE, FALSE),
-- Attendance
('CORE_ATTENDANCE_READ',          'Read Attendance',          'CORE', 'Read attendance events',              TRUE, FALSE),
('CORE_ATTENDANCE_RECORD_MANUAL', 'Record Manual Attendance', 'CORE', 'Manually record attendance events',   TRUE, FALSE),
('CORE_ATTENDANCE_PROCESS',       'Process Attendance',       'CORE', 'Mark attendance events as processed', TRUE, FALSE),
('CORE_ATTENDANCE_CORRECT',       'Correct Attendance',       'CORE', 'Correct attendance events',           TRUE, FALSE),
-- Face recognition
('CORE_FACE_ENROLL',     'Enroll Face',     'CORE', 'Enroll face images for a person',         TRUE, FALSE),
('CORE_FACE_READ',       'Read Face',       'CORE', 'Read face profile details',               TRUE, FALSE),
('CORE_FACE_DELETE',     'Delete Face',     'CORE', 'Delete a face profile',                  TRUE, FALSE),
('CORE_FACE_RECOGNIZE',  'Recognize Face',  'CORE', 'Perform face recognition queries',        TRUE, FALSE);
