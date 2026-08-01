package com.attendai.core.common.constants;

/**
 * Platform-wide constants shared across all AttendAI modules.
 *
 * This class is a non-instantiable constant holder.
 * Never use magic strings or numbers — reference these constants instead.
 */
public final class AttendAIConstants {

    private AttendAIConstants() {
        // Utility class — no instantiation
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    /** Default number of items returned per page. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Maximum allowed page size to prevent oversized payloads. */
    public static final int MAX_PAGE_SIZE = 100;

    /** Default sort direction string. */
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    // -------------------------------------------------------------------------
    // Date / time formats (for logging and serialisation)
    // -------------------------------------------------------------------------

    /** ISO 8601 datetime pattern used for logging. */
    public static final String LOG_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    /** ISO 8601 date pattern. */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /** ISO 8601 time pattern. */
    public static final String TIME_PATTERN = "HH:mm:ss";

    // -------------------------------------------------------------------------
    // Validation regex patterns
    // -------------------------------------------------------------------------

    /**
     * Basic email validation pattern.
     * Matches most valid email formats; does not attempt full RFC 5322 compliance.
     */
    public static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    /**
     * Phone number pattern — allows digits, spaces, dashes, plus sign, parentheses.
     * Maximum 30 characters enforced separately via @Size.
     */
    public static final String PHONE_PATTERN = "^[+\\d][\\d\\s\\-().]{1,29}$";

    /**
     * Permission code pattern — uppercase letters, digits, underscores only.
     * Example: SCHOOL_STUDENT_READ
     */
    public static final String PERMISSION_CODE_PATTERN = "^[A-Z0-9_]+$";

    /**
     * Role code pattern — uppercase letters, digits, underscores only.
     * Example: SCHOOL_ADMIN
     */
    public static final String ROLE_CODE_PATTERN = "^[A-Z0-9_]+$";

    // -------------------------------------------------------------------------
    // Security
    // -------------------------------------------------------------------------

    /** BCrypt work factor. Must never be lowered below 10. */
    public static final int BCRYPT_STRENGTH = 12;

    /** Default access token TTL in seconds (15 minutes). */
    public static final int DEFAULT_ACCESS_TOKEN_TTL_SECONDS = 900;

    /** Default refresh token TTL in seconds (7 days). */
    public static final int DEFAULT_REFRESH_TOKEN_TTL_SECONDS = 604_800;

    /** Default password reset token TTL in seconds (1 hour). */
    public static final int DEFAULT_RESET_TOKEN_TTL_SECONDS = 3_600;

    /** Maximum concurrent active refresh tokens per user. */
    public static final int MAX_CONCURRENT_SESSIONS = 5;

    // -------------------------------------------------------------------------
    // Attendance
    // -------------------------------------------------------------------------

    /** Default deduplication window in seconds (5 minutes). */
    public static final int DEFAULT_DEDUP_WINDOW_SECONDS = 300;

    /** Maximum seconds a station event can be in the future (clock skew tolerance). */
    public static final int MAX_FUTURE_EVENT_SECONDS = 60;

    /** Maximum hours an event can be backdated. */
    public static final int MAX_PAST_EVENT_HOURS = 24;

    // -------------------------------------------------------------------------
    // Modules
    // -------------------------------------------------------------------------

    /** Module identifier used when school module marks events as processed. */
    public static final String MODULE_SCHOOL = "school";

    /** Module identifier used in audit logs for Core. */
    public static final String MODULE_CORE = "core";
}
