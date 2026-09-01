package com.attendai.school.settings.service;

import com.attendai.school.settings.dto.SchoolSettingResponse;
import com.attendai.school.settings.dto.SchoolSettingsSummaryResponse;
import com.attendai.school.settings.dto.SetSchoolSettingRequest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * School-level settings service.
 *
 * <p>Provides typed access to school configuration overrides via a
 * cache-first, 3-level fallback mechanism:
 * <ol>
 *   <li>School-specific value from {@code school_settings} table</li>
 *   <li>In-code school module default defined in {@link SchoolSettingsDefaults}</li>
 *   <li>Platform-level default from {@code core-config} (via {@code ConfigService})</li>
 * </ol>
 *
 * <p>Read operations never throw for missing keys — they always return a default.
 * Write operations validate the key against the known key list and reject unknown keys.
 */
public interface SchoolSettingsService {

    // -------------------------------------------------------------------------
    // HTTP-facing operations
    // -------------------------------------------------------------------------

    /**
     * Returns all settings for a school.
     * Each entry shows the current effective value AND the module default.
     */
    List<SchoolSettingsSummaryResponse> listSettings(Long schoolId);

    /** Returns the effective value for a key (never throws — returns default on miss). */
    SchoolSettingResponse getSetting(Long schoolId, String key);

    /** Creates or updates a school-specific setting override. */
    SchoolSettingResponse setSetting(Long schoolId, String key,
                                      SetSchoolSettingRequest request);

    /**
     * Deletes a school-specific override and resets the key to its default.
     *
     * @return the default value that will now be used
     */
    String deleteSetting(Long schoolId, String key);

    // -------------------------------------------------------------------------
    // Internal API — used by other school sub-modules as Spring beans
    // -------------------------------------------------------------------------

    String getString(Long schoolId, String key);

    String getString(Long schoolId, String key, String defaultValue);

    boolean getBoolean(Long schoolId, String key, boolean defaultValue);

    int getInt(Long schoolId, String key, int defaultValue);

    /**
     * Parses the {@code school.attendance.mark-absent.time} setting as a {@link LocalTime}.
     *
     * @param schoolId the school
     * @return the configured mark-absent time
     */
    LocalTime getMarkAbsentTime(Long schoolId);

    /**
     * Parses {@code school.weekend.days} into a set of {@link DayOfWeek} values.
     * Used by {@code school-academic-calendar} to determine non-working days.
     *
     * @param schoolId the school
     * @return set of weekend days
     */
    Set<DayOfWeek> getWeekendDays(Long schoolId);

    /**
     * Returns whether student login is enabled for this school.
     * Reads {@code school.student.login.enabled}.
     */
    boolean isStudentLoginEnabled(Long schoolId);

    /**
     * Returns whether the four-eyes principle is required for attendance corrections.
     * Reads {@code school.corrections.four-eyes}.
     */
    boolean isFourEyesEnabled(Long schoolId);

    /**
     * Returns whether leave balance should be hard-enforced.
     * Reads {@code school.leave.balance.enforce}.
     */
    boolean isLeaveBalanceEnforced(Long schoolId);

    /**
     * Returns the dashboard cache TTL in minutes.
     * Reads {@code school.dashboard.cache.ttl-minutes}.
     */
    int getDashboardCacheTtlMinutes(Long schoolId);
}
