-- V6: Seed built-in system roles
INSERT INTO roles (code, name, description, is_system, is_deleted)
VALUES ('SYSTEM_ADMIN', 'System Administrator',
        'Full platform access; system-level operations only', TRUE, FALSE);
