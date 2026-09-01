-- V102: Seed school module permissions
INSERT IGNORE INTO permissions (code, name, module, description, is_system, is_deleted) VALUES
-- School management
('SCHOOL_SCHOOL_CREATE', 'Create School',  'SCHOOL', 'Register a new school',       FALSE, FALSE),
('SCHOOL_SCHOOL_READ',   'Read School',    'SCHOOL', 'Read school details',          FALSE, FALSE),
('SCHOOL_SCHOOL_UPDATE', 'Update School',  'SCHOOL', 'Update school profile/status', FALSE, FALSE),
('SCHOOL_SCHOOL_DELETE', 'Delete School',  'SCHOOL', 'Soft-delete a school',         FALSE, FALSE),
-- Administrator management
('SCHOOL_ADMINISTRATOR_CREATE', 'Create Administrator', 'SCHOOL', 'Add a school administrator', FALSE, FALSE),
('SCHOOL_ADMINISTRATOR_READ',   'Read Administrator',   'SCHOOL', 'Read administrator details', FALSE, FALSE),
('SCHOOL_ADMINISTRATOR_UPDATE', 'Update Administrator', 'SCHOOL', 'Update administrator',       FALSE, FALSE),
('SCHOOL_ADMINISTRATOR_DELETE', 'Delete Administrator', 'SCHOOL', 'Remove administrator',       FALSE, FALSE),
-- Teacher management
('SCHOOL_TEACHER_CREATE', 'Create Teacher', 'SCHOOL', 'Add a teacher',        FALSE, FALSE),
('SCHOOL_TEACHER_READ',   'Read Teacher',   'SCHOOL', 'Read teacher details', FALSE, FALSE),
('SCHOOL_TEACHER_UPDATE', 'Update Teacher', 'SCHOOL', 'Update a teacher',     FALSE, FALSE),
('SCHOOL_TEACHER_DELETE', 'Delete Teacher', 'SCHOOL', 'Remove a teacher',     FALSE, FALSE),
-- Student management
('SCHOOL_STUDENT_CREATE', 'Enroll Student', 'SCHOOL', 'Enroll a student',     FALSE, FALSE),
('SCHOOL_STUDENT_READ',   'Read Student',   'SCHOOL', 'Read student details', FALSE, FALSE),
('SCHOOL_STUDENT_UPDATE', 'Update Student', 'SCHOOL', 'Update a student',     FALSE, FALSE),
('SCHOOL_STUDENT_DELETE', 'Delete Student', 'SCHOOL', 'Remove a student',     FALSE, FALSE),
-- Academic year
('SCHOOL_ACADEMIC_YEAR_CREATE', 'Create Academic Year', 'SCHOOL', 'Create an academic year', FALSE, FALSE),
('SCHOOL_ACADEMIC_YEAR_READ',   'Read Academic Year',   'SCHOOL', 'Read academic year',      FALSE, FALSE),
('SCHOOL_ACADEMIC_YEAR_UPDATE', 'Update Academic Year', 'SCHOOL', 'Update academic year',    FALSE, FALSE),
('SCHOOL_ACADEMIC_YEAR_DELETE', 'Delete Academic Year', 'SCHOOL', 'Delete academic year',    FALSE, FALSE),
-- Academic calendar
('SCHOOL_CALENDAR_MANAGE', 'Manage Academic Calendar', 'SCHOOL', 'Manage holidays and working days', FALSE, FALSE),
('SCHOOL_CALENDAR_READ',   'Read Academic Calendar',   'SCHOOL', 'Read calendar entries',            FALSE, FALSE),
-- Class / Section / Subject
('SCHOOL_CLASS_CREATE',   'Create Class',   'SCHOOL', 'Create a class',   FALSE, FALSE),
('SCHOOL_CLASS_READ',     'Read Class',     'SCHOOL', 'Read class details', FALSE, FALSE),
('SCHOOL_CLASS_UPDATE',   'Update Class',   'SCHOOL', 'Update a class',   FALSE, FALSE),
('SCHOOL_CLASS_DELETE',   'Delete Class',   'SCHOOL', 'Delete a class',   FALSE, FALSE),
('SCHOOL_SECTION_CREATE', 'Create Section', 'SCHOOL', 'Create a section', FALSE, FALSE),
('SCHOOL_SECTION_READ',   'Read Section',   'SCHOOL', 'Read section',     FALSE, FALSE),
('SCHOOL_SECTION_UPDATE', 'Update Section', 'SCHOOL', 'Update a section', FALSE, FALSE),
('SCHOOL_SECTION_MANAGE', 'Manage Section Enrollment', 'SCHOOL', 'Enroll/remove students', FALSE, FALSE),
('SCHOOL_SUBJECT_CREATE', 'Create Subject', 'SCHOOL', 'Create a subject', FALSE, FALSE),
('SCHOOL_SUBJECT_READ',   'Read Subject',   'SCHOOL', 'Read subject',     FALSE, FALSE),
('SCHOOL_SUBJECT_UPDATE', 'Update Subject', 'SCHOOL', 'Update a subject', FALSE, FALSE),
('SCHOOL_SUBJECT_DELETE', 'Delete Subject', 'SCHOOL', 'Delete a subject', FALSE, FALSE),
-- Teacher assignment / Timetable
('SCHOOL_TEACHER_ASSIGNMENT_CREATE', 'Create Teacher Assignment', 'SCHOOL', 'Assign teacher to section-subject', FALSE, FALSE),
('SCHOOL_TEACHER_ASSIGNMENT_READ',   'Read Teacher Assignment',   'SCHOOL', 'Read teacher assignments',           FALSE, FALSE),
('SCHOOL_TEACHER_ASSIGNMENT_UPDATE', 'Update Teacher Assignment', 'SCHOOL', 'Update a teacher assignment',        FALSE, FALSE),
('SCHOOL_TEACHER_ASSIGNMENT_DELETE', 'Delete Teacher Assignment', 'SCHOOL', 'Delete teacher assignment',          FALSE, FALSE),
('SCHOOL_TIMETABLE_MANAGE', 'Manage Timetable', 'SCHOOL', 'Create/update timetable entries', FALSE, FALSE),
('SCHOOL_TIMETABLE_READ',   'Read Timetable',   'SCHOOL', 'Read timetable',                  FALSE, FALSE),
-- Attendance
('SCHOOL_ATTENDANCE_READ',       'Read Attendance',      'SCHOOL', 'Read attendance records',        FALSE, FALSE),
('SCHOOL_ATTENDANCE_OVERRIDE',   'Override Attendance',  'SCHOOL', 'Override an attendance record',  FALSE, FALSE),
('SCHOOL_ATTENDANCE_PROCESS',    'Process Attendance',   'SCHOOL', 'Mark attendance events as processed', FALSE, FALSE),
('SCHOOL_ATTENDANCE_CORRECT',    'Correct Attendance',   'SCHOOL', 'Submit attendance correction',   FALSE, FALSE),
('SCHOOL_ATTENDANCE_RULES_READ',   'Read Attendance Rules',   'SCHOOL', 'Read attendance rules',   FALSE, FALSE),
('SCHOOL_ATTENDANCE_RULES_MANAGE', 'Manage Attendance Rules', 'SCHOOL', 'Manage attendance rules', FALSE, FALSE),
('SCHOOL_ATTENDANCE_CORRECTION_REQUEST', 'Request Correction',  'SCHOOL', 'Submit correction request', FALSE, FALSE),
('SCHOOL_ATTENDANCE_CORRECTION_READ',    'Read Correction',     'SCHOOL', 'Read correction requests',  FALSE, FALSE),
('SCHOOL_ATTENDANCE_CORRECTION_APPROVE', 'Approve Correction',  'SCHOOL', 'Approve/reject correction', FALSE, FALSE),
('SCHOOL_ATTENDANCE_REPORT_READ',        'Read Attendance Report', 'SCHOOL', 'Read attendance reports', FALSE, FALSE),
-- Leave
('SCHOOL_LEAVE_REQUEST', 'Request Leave', 'SCHOOL', 'Submit a leave application',    FALSE, FALSE),
('SCHOOL_LEAVE_READ',    'Read Leave',    'SCHOOL', 'Read leave applications',       FALSE, FALSE),
('SCHOOL_LEAVE_MANAGE',  'Manage Leave',  'SCHOOL', 'Approve/reject/revoke leaves',  FALSE, FALSE),
-- Dashboard / Settings
('SCHOOL_DASHBOARD_READ',             'Read Dashboard',             'SCHOOL', 'Read school dashboard',          FALSE, FALSE),
('SCHOOL_TEACHER_DASHBOARD_READ',     'Read Teacher Dashboard',     'SCHOOL', 'Read own-section dashboard',     FALSE, FALSE),
('SCHOOL_SETTINGS_READ',              'Read Settings',              'SCHOOL', 'Read school settings',           FALSE, FALSE),
('SCHOOL_SETTINGS_MANAGE',            'Manage Settings',            'SCHOOL', 'Update school settings',         FALSE, FALSE);
