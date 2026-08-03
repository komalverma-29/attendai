package com.attendai.core.config.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.config.dto.SetConfigRequest;
import com.attendai.core.config.dto.SystemConfigResponse;
import com.attendai.core.config.dto.SystemConfigSummaryResponse;
import com.attendai.core.config.entity.SystemConfig;
import com.attendai.core.config.exception.ConfigKeyNotFoundException;
import com.attendai.core.config.mapper.SystemConfigMapper;
import com.attendai.core.config.repository.SystemConfigRepository;
import com.attendai.core.config.service.ConfigService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for system configuration management.
 *
 * Base path: /api/v1/core/config
 *
 * Reading config requires {@code CORE_CONFIG_READ}.
 * Writing or deleting config requires {@code CORE_CONFIG_WRITE} — restricted
 * to platform administrators only.
 */
@RestController
@RequestMapping("/api/v1/core/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService            configService;
    private final SystemConfigRepository   systemConfigRepository;
    private final SystemConfigMapper       systemConfigMapper;

    /**
     * GET /api/v1/core/config
     * List all configuration entries, optionally filtered by module or key search.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CORE_CONFIG_READ')")
    public ResponseEntity<PageResponse<SystemConfigSummaryResponse>> listConfigs(
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "search", required = false) String search,
            @Valid PageRequestParams pageParams) {

        return ResponseEntity.ok(PageResponse.of(
                systemConfigRepository
                        .findByFilters(module, search, pageParams.toPageable())
                        .map(systemConfigMapper::toSummaryResponse)));
    }

    /**
     * GET /api/v1/core/config/{key}
     * Retrieve a single configuration entry by key.
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('CORE_CONFIG_READ')")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfig(
            @PathVariable("key") String key) {

        SystemConfig config = systemConfigRepository
                .findByConfigKey(key.trim().toLowerCase())
                .orElseThrow(() -> new ConfigKeyNotFoundException(key));

        return ResponseEntity.ok(ApiResponse.success(systemConfigMapper.toResponse(config)));
    }

    /**
     * PUT /api/v1/core/config/{key}
     * Create or update a configuration key (upsert semantics).
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('CORE_CONFIG_WRITE')")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> setConfig(
            @PathVariable("key") String key,
            @Valid @RequestBody SetConfigRequest request) {

        configService.set(key, request.getValue(), request.getModule(), request.getDescription());

        SystemConfig saved = systemConfigRepository
                .findByConfigKey(key.trim().toLowerCase())
                .orElseThrow(() -> new ConfigKeyNotFoundException(key));

        return ResponseEntity.ok(ApiResponse.success(systemConfigMapper.toResponse(saved)));
    }

    /**
     * DELETE /api/v1/core/config/{key}
     * Remove a configuration key. After deletion reads fall back to defaults.
     */
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('CORE_CONFIG_WRITE')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteConfig(
            @PathVariable("key") String key) {

        // Read current value for the response message before deleting
        String current = configService.getString(key, "(not set)");
        configService.delete(key);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Setting reset to default", "deletedValue", current)));
    }
}
