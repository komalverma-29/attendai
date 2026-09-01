package com.attendai.school.settings.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.config.service.ConfigService;
import com.attendai.school.settings.cache.SchoolSettingsCache;
import com.attendai.school.settings.dto.SchoolSettingsSummaryResponse;
import com.attendai.school.settings.dto.SetSchoolSettingRequest;
import com.attendai.school.settings.entity.SchoolSetting;
import com.attendai.school.settings.mapper.SchoolSettingMapper;
import com.attendai.school.settings.repository.SchoolSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolSettingsServiceImplTest {

    @Mock SchoolSettingRepository schoolSettingRepository;
    @Mock SchoolSettingMapper     schoolSettingMapper;
    @Mock ConfigService           configService;
    @Mock AuditService            auditService;

    private SchoolSettingsCache     schoolSettingsCache;
    private SchoolSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        schoolSettingsCache = new SchoolSettingsCache();
        service = new SchoolSettingsServiceImpl(
                schoolSettingRepository, schoolSettingsCache,
                schoolSettingMapper, configService, auditService);
    }

    // -------------------------------------------------------------------------
    // getString — three-level fallback
    // -------------------------------------------------------------------------

    @Test
    void getString_shouldReturnCachedValue_onCacheHit() {
        schoolSettingsCache.put(1L, "school.weekend.days", "FRI,SAT");

        String result = service.getString(1L, "school.weekend.days");

        assertThat(result).isEqualTo("FRI,SAT");
        verify(schoolSettingRepository, never()).findBySchoolIdAndSettingKey(any(), any());
    }

    @Test
    void getString_shouldReturnDbValue_onCacheMiss() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.weekend.days"))
                .thenReturn(Optional.of(buildSetting(1L, "school.weekend.days", "FRI,SAT")));

        String result = service.getString(1L, "school.weekend.days");

        assertThat(result).isEqualTo("FRI,SAT");
    }

    @Test
    void getString_shouldPopulateCache_afterDbLoad() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.weekend.days"))
                .thenReturn(Optional.of(buildSetting(1L, "school.weekend.days", "FRI,SAT")));

        service.getString(1L, "school.weekend.days");

        // Second call should be a cache hit
        assertThat(schoolSettingsCache.get(1L, "school.weekend.days")).isPresent().contains("FRI,SAT");
    }

    @Test
    void getString_shouldReturnModuleDefault_whenNoDbOverride() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.weekend.days"))
                .thenReturn(Optional.empty());

        String result = service.getString(1L, "school.weekend.days");

        // Module default is SAT,SUN
        assertThat(result).isEqualTo("SAT,SUN");
        verify(configService, never()).getString(anyString(), anyString());
    }

    @Test
    void getString_withDefault_shouldReturnProvidedDefault_whenKeyUnrecognised() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "unknown.key"))
                .thenReturn(Optional.empty());
        when(configService.getString("unknown.key", null)).thenReturn(null);

        String result = service.getString(1L, "unknown.key", "my-fallback");

        assertThat(result).isEqualTo("my-fallback");
    }

    // -------------------------------------------------------------------------
    // getBoolean
    // -------------------------------------------------------------------------

    @Test
    void getBoolean_shouldReturnTrue_forTrueValue() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.attendance.processing.enabled"))
                .thenReturn(Optional.of(buildSetting(1L, "school.attendance.processing.enabled", "true")));

        assertThat(service.getBoolean(1L, "school.attendance.processing.enabled", false)).isTrue();
    }

    @Test
    void getBoolean_shouldReturnFalse_forFalseValue() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.student.login.enabled"))
                .thenReturn(Optional.of(buildSetting(1L, "school.student.login.enabled", "false")));

        assertThat(service.isStudentLoginEnabled(1L)).isFalse();
    }

    @Test
    void getBoolean_shouldReturnDefault_whenKeyAbsent() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        // Module default for school.student.login.enabled is "false"
        assertThat(service.isStudentLoginEnabled(1L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // getWeekendDays
    // -------------------------------------------------------------------------

    @Test
    void getWeekendDays_shouldParseCorrectly_forSatSun() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.weekend.days"))
                .thenReturn(Optional.empty());

        Set<DayOfWeek> result = service.getWeekendDays(1L);

        assertThat(result).containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void getWeekendDays_shouldIncludeFriday_whenConfigured() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.weekend.days"))
                .thenReturn(Optional.of(buildSetting(1L, "school.weekend.days", "FRI,SAT")));

        Set<DayOfWeek> result = service.getWeekendDays(1L);

        assertThat(result).containsExactlyInAnyOrder(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        assertThat(result).doesNotContain(DayOfWeek.SUNDAY);
    }

    // -------------------------------------------------------------------------
    // getMarkAbsentTime
    // -------------------------------------------------------------------------

    @Test
    void getMarkAbsentTime_shouldReturnDefault_11_00_whenNotSet() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        LocalTime result = service.getMarkAbsentTime(1L);

        assertThat(result).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void getMarkAbsentTime_shouldParseCustomTime() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.attendance.mark-absent.time"))
                .thenReturn(Optional.of(buildSetting(1L, "school.attendance.mark-absent.time", "10:30")));

        LocalTime result = service.getMarkAbsentTime(1L);

        assertThat(result).isEqualTo(LocalTime.of(10, 30));
    }

    // -------------------------------------------------------------------------
    // setSetting — validation and caching
    // -------------------------------------------------------------------------

    @Test
    void setSetting_shouldSave_andEvictCache_whenValidKey() {
        // Pre-populate cache with old value
        schoolSettingsCache.put(1L, "school.weekend.days", "SAT,SUN");

        when(schoolSettingRepository.findBySchoolIdAndSettingKey(1L, "school.weekend.days"))
                .thenReturn(Optional.empty());

        SchoolSetting saved = buildSetting(1L, "school.weekend.days", "FRI,SAT");
        when(schoolSettingRepository.save(any())).thenReturn(saved);
        when(schoolSettingMapper.toResponse(any(), anyString())).thenReturn(null);

        SetSchoolSettingRequest req = new SetSchoolSettingRequest();
        req.setValue("FRI,SAT");

        service.setSetting(1L, "school.weekend.days", req);

        // Cache must be evicted so next read goes to DB
        assertThat(schoolSettingsCache.get(1L, "school.weekend.days")).isEmpty();
        verify(schoolSettingRepository).save(any(SchoolSetting.class));
        verify(auditService).log(any());
    }

    @Test
    void setSetting_shouldThrow400_whenKeyIsUnrecognised() {
        SetSchoolSettingRequest req = new SetSchoolSettingRequest();
        req.setValue("some-value");

        assertThatThrownBy(() -> service.setSetting(1L, "unknown.random.key", req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown school setting key");

        verify(schoolSettingRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteSetting
    // -------------------------------------------------------------------------

    @Test
    void deleteSetting_shouldEvictCache_andReturnDefault() {
        // Pre-populate cache
        schoolSettingsCache.put(1L, "school.weekend.days", "FRI,SAT");

        String defaultValue = service.deleteSetting(1L, "school.weekend.days");

        assertThat(defaultValue).isEqualTo("SAT,SUN");
        assertThat(schoolSettingsCache.get(1L, "school.weekend.days")).isEmpty();
        verify(schoolSettingRepository).deleteBySchoolIdAndSettingKey(1L, "school.weekend.days");
        verify(auditService).log(any());
    }

    @Test
    void deleteSetting_shouldThrow400_whenKeyIsUnrecognised() {
        assertThatThrownBy(() -> service.deleteSetting(1L, "unknown.key"))
                .isInstanceOf(ValidationException.class);
    }

    // -------------------------------------------------------------------------
    // listSettings
    // -------------------------------------------------------------------------

    @Test
    void listSettings_shouldReturn10Entries_oneForEachKnownKey() {
        when(schoolSettingRepository.findBySchoolId(1L)).thenReturn(List.of());

        List<SchoolSettingsSummaryResponse> result = service.listSettings(1L);

        assertThat(result).hasSize(SchoolSettingsDefaults.DEFAULTS.size());
    }

    @Test
    void listSettings_shouldShowOverriddenValue_whenSchoolHasOverride() {
        SchoolSetting override = buildSetting(1L, "school.weekend.days", "FRI,SAT");
        when(schoolSettingRepository.findBySchoolId(1L)).thenReturn(List.of(override));

        List<SchoolSettingsSummaryResponse> result = service.listSettings(1L);

        SchoolSettingsSummaryResponse entry = result.stream()
                .filter(r -> "school.weekend.days".equals(r.getKey()))
                .findFirst()
                .orElseThrow();

        assertThat(entry.getValue()).isEqualTo("FRI,SAT");
        assertThat(entry.getDefaultValue()).isEqualTo("SAT,SUN");
    }

    // -------------------------------------------------------------------------
    // SchoolSettingsCache — unit tests
    // -------------------------------------------------------------------------

    @Test
    void cache_shouldReturnEmpty_onMiss() {
        assertThat(schoolSettingsCache.get(99L, "school.weekend.days")).isEmpty();
    }

    @Test
    void cache_shouldReturnValue_afterPut() {
        schoolSettingsCache.put(1L, "school.weekend.days", "SAT,SUN");
        assertThat(schoolSettingsCache.get(1L, "school.weekend.days")).isPresent().contains("SAT,SUN");
    }

    @Test
    void cache_shouldNotLeak_acrossSchools() {
        schoolSettingsCache.put(1L, "school.weekend.days", "SAT,SUN");

        // Different school — must not see school 1's value
        assertThat(schoolSettingsCache.get(2L, "school.weekend.days")).isEmpty();
    }

    @Test
    void cache_scheduledInvalidation_shouldClearAllEntries() {
        schoolSettingsCache.put(1L, "school.weekend.days", "SAT,SUN");
        schoolSettingsCache.put(2L, "school.attendance.mark-absent.time", "11:00");
        assertThat(schoolSettingsCache.size()).isEqualTo(2);

        schoolSettingsCache.scheduledInvalidation();

        assertThat(schoolSettingsCache.size()).isZero();
    }

    // -------------------------------------------------------------------------
    // isFourEyesEnabled / isLeaveBalanceEnforced
    // -------------------------------------------------------------------------

    @Test
    void isFourEyesEnabled_shouldReturnTrue_byDefault() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        assertThat(service.isFourEyesEnabled(1L)).isTrue();
    }

    @Test
    void isLeaveBalanceEnforced_shouldReturnFalse_byDefault() {
        when(schoolSettingRepository.findBySchoolIdAndSettingKey(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        assertThat(service.isLeaveBalanceEnforced(1L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SchoolSetting buildSetting(Long schoolId, String key, String value) {
        SchoolSetting s = SchoolSetting.builder()
                .schoolId(schoolId)
                .settingKey(key)
                .settingValue(value)
                .build();
        s.setId(1L);
        return s;
    }
}
