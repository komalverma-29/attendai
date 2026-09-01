-- V101: Seed school module roles
INSERT IGNORE INTO roles (code, name, description, is_system, is_deleted) VALUES
('SCHOOL_ADMIN',   'School Administrator',
 'Full access to school management, enrollment, and attendance operations', FALSE, FALSE),
('SCHOOL_TEACHER', 'Teacher',
 'Access to class timetable, attendance marking, and student data', FALSE, FALSE),
('SCHOOL_STUDENT', 'Student',
 'Optional login role for students to view their attendance and leave records', FALSE, FALSE);
