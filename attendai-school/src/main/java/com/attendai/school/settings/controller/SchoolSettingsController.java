package com.attendai.school.settings.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.settings.dto.SchoolSettingResponse;
import com.attendai.school.settings.dto.SchoolSettingsSummaryResponse;
import com.attendai.school.settings.dto.SetSchoolSettingRequest;
import com.attendai.school.settings.service.SchoolSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for school-level settings management.
 *
 * Base path: /api/v1/school/schools/{schoolId}/settings
 */
@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/settings")
@RequiredArgsConstructor
public class SchoolSettingsController {

    private final SchoolSettingsService schoolSettingsService;

    /**
     * GET /api/v1/school/schools/{schoolId}/settings
     * Lists all settings for a school with effective values and defaults.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_SETTINGS_READ')")
    public ResponseEntity<ApiResponse<List<SchoolSettingsSummaryResponse>>> listSettings(
            @PathVariable("schoolId") Long schoolId) {
        return ResponseEntity.ok(ApiResponse.success(
                schoolSettingsService.listSettings(schoolId)));
    }

    /**
     * GET /api/v1/school/schools/{schoolId}/settings/{key}
     * Returns the effective value for a key (never throws — returns default on miss).
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('SCHOOL_SETTINGS_READ')")
    public ResponseEntity<ApiResponse<SchoolSettingResponse>> getSetting(
            @PathVariable("schoolId") Long   schoolId,
            @PathVariable("key")      String key) {
        return ResponseEntity.ok(ApiResponse.success(
                schoolSettingsService.getSetting(schoolId, key)));
    }

    /**
     * PUT /api/v1/school/schools/{schoolId}/settings/{key}
     * Creates or updates a school-specific setting override.
     * Returns 400 for unknown keys or invalid values.
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('SCHOOL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<SchoolSettingResponse>> setSetting(
            @PathVariable("schoolId")  Long                   schoolId,
            @PathVariable("key")       String                 key,
            @Valid @RequestBody        SetSchoolSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                schoolSettingsService.setSetting(schoolId, key, request)));
    }

    /**
     * DELETE /api/v1/school/schools/{schoolId}/settings/{key}
     * Removes the school-specific override and resets the key to its default.
     */
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('SCHOOL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteSetting(
            @PathVariable("schoolId") Long   schoolId,
            @PathVariable("key")      String key) {
        String defaultValue = schoolSettingsService.deleteSetting(schoolId, key);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message",      "Setting reset to default",
                "defaultValue", defaultValue)));
    }
}
