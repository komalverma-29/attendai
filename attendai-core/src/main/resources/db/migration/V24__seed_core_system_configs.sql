-- V24: Seed default Core system configuration keys
--
-- These are the platform-wide defaults consumed by Core modules.
-- Business modules seed their own keys during their own startup.
-- Existing keys are NOT overwritten by this migration.

INSERT IGNORE INTO system_configs (config_key, config_value, module, description, is_encrypted) VALUES
-- Attendance engine settings
('attendance.dedup.window-seconds',     '300',      'attendance', 'Deduplication window in seconds (default: 5 minutes)', FALSE),
('attendance.event.max-future-seconds', '60',       'attendance', 'Max seconds an attendance event can be in the future',  FALSE),
('attendance.event.max-past-hours',     '24',       'attendance', 'Max hours an attendance event can be backdated',         FALSE),

-- Face recognition settings
('face.recognition.threshold',             '0.85', 'face', 'Minimum confidence score for a positive face match (0.0-1.0)', FALSE),
('face.recognition.max-images-per-profile','10',   'face', 'Maximum face images allowed per face profile',                FALSE),
('face.liveness.enabled',                  'false','face', 'Enable global liveness detection for face recognition',        FALSE),

-- Notification delivery settings
('notification.retry.max-attempts',   '3',   'notification', 'Maximum notification delivery retry attempts',     FALSE),
('notification.retry.interval-seconds','300','notification', 'Notification retry scheduler interval in seconds', FALSE),

-- File management settings
('file.max-size-bytes', '10485760', 'file', 'Maximum allowed file upload size in bytes (default: 10 MB)', FALSE),

-- Authentication settings
('security.max-sessions',              '5',    'auth', 'Maximum concurrent active refresh tokens per user',        FALSE),
('security.reset-token-expiry-seconds','3600', 'auth', 'Password reset token TTL in seconds (default: 1 hour)',    FALSE);
