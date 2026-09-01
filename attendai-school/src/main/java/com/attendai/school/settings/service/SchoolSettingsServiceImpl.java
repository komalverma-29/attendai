package com.attendai.school.settings.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.config.service.ConfigService;
import com.attendai.school.settings.cache.SchoolSettingsCache;
import com.attendai.school.settings.dto.SchoolSettingResponse;
import com.attendai.school.settings.dto.SchoolSettingsSummaryResponse;
import com.attendai.school.settings.dto.SetSchoolSettingRequest;
import com.attendai.school.settings.entity.SchoolSetting;
import com.attendai.school.settings.mapper.SchoolSettingMapper;
import com.attendai.school.settings.repository.SchoolSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolSettingsServiceImpl implements SchoolSettingsService {

    private static final String MODULE = "school";

    private final SchoolSettingRepository schoolSettingRepository;
    private final SchoolSettingsCache     schoolSettingsCache;
    private final SchoolSettingMapper     schoolSettingMapper;
    private final ConfigService           configService;
    private final AuditService            auditService;

    // -------------------------------------------------------------------------
    // HTTP-facing operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SchoolSettingsSummaryResponse> listSettings(Long schoolId) {
        // Load all school-specific overrides into a map for O(1) lookup
        var overrides = new java.util.HashMap<String, SchoolSetting>();
        schoolSettingRepository.findBySchoolId(schoolId)
                .forEach(s -> overrides.put(s.getSettingKey(), s));

        List<SchoolSettingsSummaryResponse> results = new ArrayList<>();

        for (var entry : SchoolSettingsDefaults.DEFAULTS.entrySet()) {
            String key          = entry.getKey();
            String defaultValue = entry.getValue();
            SchoolSetting override = overrides.get(key);

            String effectiveValue = override != null
                    ? override.getSettingValue()
                    : defaultValue;

            results.add(SchoolSettingsSummaryResponse.builder()
                    .key(key)
                    .value(effectiveValue)
                    .defaultValue(defaultValue)
                    .description(override != null ? override.getDescription() : null)
                    .build());
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolSettingResponse getSetting(Long schoolId, String key) {
        String defaultValue = SchoolSettingsDefaults.getDefault(key);
        String effectiveValue = resolveEffectiveValue(schoolId, key);

        // For the response: try to find the school override for audit timestamps
        Optional<SchoolSetting> override =
                schoolSettingRepository.findBySchoolIdAndSettingKey(schoolId, key);

        if (override.isPresent()) {
            return schoolSettingMapper.toResponse(override.get(), defaultValue);
        }

        // No school override — return a synthetic response with the default value
        return SchoolSettingResponse.builder()
                .key(key)
                .value(effectiveValue)
                .defaultValue(defaultValue)
                .description(null)
                .createdAt(null)
                .updatedAt(null)
                .build();
    }

    @Override
    @Transactional
    public SchoolSettingResponse setSetting(Long schoolId, String key,
                                             SetSchoolSettingRequest request) {
        // Validate key is recognised
        if (!SchoolSettingsDefaults.isKnownKey(key)) {
            throw new ValidationException("Unknown school setting key: '" + key + "'");
        }

        // Capture old value for audit
        String oldValue = resolveEffectiveValue(schoolId, key);
        String defaultValue = SchoolSettingsDefaults.getDefault(key);

        // Upsert
        SchoolSetting setting = schoolSettingRepository
                .findBySchoolIdAndSettingKey(schoolId, key)
                .orElseGet(() -> SchoolSetting.builder()
                        .schoolId(schoolId)
                        .settingKey(key)
                        .build());

        setting.setSettingValue(request.getValue());
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }

        SchoolSetting saved = schoolSettingRepository.save(setting);

        // Evict from cache so next read picks up new value
        schoolSettingsCache.evict(schoolId, key);

        log.info("School setting set | schoolId={} key={}", schoolId, key);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHOOL_SETTING_CHANGED")
                .module(MODULE)
                .resourceType("SchoolSetting")
                .resourceId(schoolId + ":" + key)
                .details("{\"schoolId\":" + schoolId
                        + ",\"key\":\"" + key + "\""
                        + ",\"old_value\":" + jsonString(oldValue)
                        + ",\"new_value\":" + jsonString(request.getValue()) + "}")
                .build());

        return schoolSettingMapper.toResponse(saved, defaultValue);
    }

    @Override
    @Transactional
    public String deleteSetting(Long schoolId, String key) {
        if (!SchoolSettingsDefaults.isKnownKey(key)) {
            throw new ValidationException("Unknown school setting key: '" + key + "'");
        }

        schoolSettingRepository.deleteBySchoolIdAndSettingKey(schoolId, key);
        schoolSettingsCache.evict(schoolId, key);

        String defaultValue = SchoolSettingsDefaults.getDefault(key);

        log.info("School setting deleted (reset to default) | schoolId={} key={}", schoolId, key);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHOOL_SETTING_DELETED")
                .module(MODULE)
                .resourceType("SchoolSetting")
                .resourceId(schoolId + ":" + key)
                .details("{\"schoolId\":" + schoolId + ",\"key\":\"" + key + "\"}")
                .build());

        return defaultValue;
    }

    // -------------------------------------------------------------------------
    // Internal API
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public String getString(Long schoolId, String key) {
        return resolveEffectiveValue(schoolId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(Long schoolId, String key, String defaultValue) {
        String value = resolveEffectiveValue(schoolId, key);
        return value != null ? value : defaultValue;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(Long schoolId, String key, boolean defaultValue) {
        String value = resolveEffectiveValue(schoolId, key);
        if (value == null) return defaultValue;
        String lower = value.trim().toLowerCase();
        if ("true".equals(lower))  return true;
        if ("false".equals(lower)) return false;
        log.warn("Cannot parse boolean for key '{}' (value='{}'), using default {}", key, value, defaultValue);
        return defaultValue;
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(Long schoolId, String key, int defaultValue) {
        String value = resolveEffectiveValue(schoolId, key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse int for key '{}' (value='{}'), using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LocalTime getMarkAbsentTime(Long schoolId) {
        String value = resolveEffectiveValue(schoolId, "school.attendance.mark-absent.time");
        if (value == null) return LocalTime.of(11, 0);
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            log.warn("Cannot parse LocalTime for mark-absent.time (value='{}'), using 11:00", value);
            return LocalTime.of(11, 0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<DayOfWeek> getWeekendDays(Long schoolId) {
        String value = resolveEffectiveValue(schoolId, "school.weekend.days");
        if (value == null || value.isBlank()) {
            return Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }
        Set<DayOfWeek> days = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            String code = part.trim().toUpperCase();
            switch (code) {
                case "MON" -> days.add(DayOfWeek.MONDAY);
                case "TUE" -> days.add(DayOfWeek.TUESDAY);
                case "WED" -> days.add(DayOfWeek.WEDNESDAY);
                case "THU" -> days.add(DayOfWeek.THURSDAY);
                case "FRI" -> days.add(DayOfWeek.FRIDAY);
                case "SAT" -> days.add(DayOfWeek.SATURDAY);
                case "SUN" -> days.add(DayOfWeek.SUNDAY);
                default    -> log.warn("Unrecognised day code '{}' in school.weekend.days for schoolId={}", code, schoolId);
            }
        }
        return days.isEmpty() ? Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) : days;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStudentLoginEnabled(Long schoolId) {
        return getBoolean(schoolId, "school.student.login.enabled", false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFourEyesEnabled(Long schoolId) {
        return getBoolean(schoolId, "school.corrections.four-eyes", true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLeaveBalanceEnforced(Long schoolId) {
        return getBoolean(schoolId, "school.leave.balance.enforce", false);
    }

    @Override
    @Transactional(readOnly = true)
    public int getDashboardCacheTtlMinutes(Long schoolId) {
        return getInt(schoolId, "school.dashboard.cache.ttl-minutes", 2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Three-level effective value resolution per spec section 7:
     * 1. School DB override (cache-first)
     * 2. In-code module default
     * 3. core-config platform default
     */
    private String resolveEffectiveValue(Long schoolId, String key) {
        // 1. Cache hit
        Optional<String> cached = schoolSettingsCache.get(schoolId, key);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. DB lookup (school override)
        Optional<String> fromDb = schoolSettingRepository
                .findBySchoolIdAndSettingKey(schoolId, key)
                .map(SchoolSetting::getSettingValue);

        if (fromDb.isPresent()) {
            schoolSettingsCache.put(schoolId, key, fromDb.get());
            return fromDb.get();
        }

        // 3. In-code module default
        String moduleDefault = SchoolSettingsDefaults.getDefault(key);
        if (moduleDefault != null) {
            return moduleDefault;
        }

        // 4. core-config fallback (for keys not in the module defaults map)
        String coreDefault = configService.getString(key, null);
        if (coreDefault != null) {
            return coreDefault;
        }

        return null;
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
