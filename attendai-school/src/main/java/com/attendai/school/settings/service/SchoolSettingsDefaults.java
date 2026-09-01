package com.attendai.school.settings.service;

import java.util.Map;

/**
 * In-code defaults for all recognised school setting keys.
 *
 * <p>These are used as the second level of the three-level fallback:
 * <ol>
 *   <li>School DB override</li>
 *   <li>This defaults map (module level)</li>
 *   <li>core-config platform default</li>
 * </ol>
 *
 * <p>All 10 keys from the specification are enumerated here.
 */
public final class SchoolSettingsDefaults {

    private SchoolSettingsDefaults() {}

    /** All recognised setting keys mapped to their default values. */
    public static final Map<String, String> DEFAULTS = Map.of(
            "school.weekend.days",                  "SAT,SUN",
            "school.attendance.processing.enabled", "true",
            "school.attendance.mark-absent.time",   "11:00",
            "school.attendance.notify.absent",      "true",
            "school.attendance.notify.late",        "false",
            "school.student.login.enabled",         "false",
            "school.leave.balance.enforce",         "false",
            "school.corrections.four-eyes",         "true",
            "school.dashboard.cache.ttl-minutes",   "2",
            "school.attendance.consecutive-alert",  "3"
    );

    /** Returns true when the given key is a recognised school setting. */
    public static boolean isKnownKey(String key) {
        return DEFAULTS.containsKey(key);
    }

    /** Returns the module default for the given key, or null if the key is unrecognised. */
    public static String getDefault(String key) {
        return DEFAULTS.get(key);
    }
}
