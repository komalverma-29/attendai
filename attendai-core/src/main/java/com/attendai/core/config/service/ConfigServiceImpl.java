package com.attendai.core.config.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.config.cache.ConfigCache;
import com.attendai.core.config.entity.SystemConfig;
import com.attendai.core.config.exception.ConfigKeyNotFoundException;
import com.attendai.core.config.exception.ConfigValueConversionException;
import com.attendai.core.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link ConfigService}.
 *
 * <p>Read path: cache → DB → default / throw.
 * <p>Write path: DB upsert → cache update → audit log.
 *
 * <p>Security: config key names containing sensitive-sounding words are
 * masked as {@code [REDACTED]} in audit log details.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    /** Key name fragments that suggest a sensitive value — mask in audit logs. */
    private static final Set<String> SENSITIVE_KEY_FRAGMENTS =
            Set.of("password", "token", "secret", "key", "credential");

    private final SystemConfigRepository systemConfigRepository;
    private final ConfigCache            configCache;
    private final AuditService           auditService;

    // -------------------------------------------------------------------------
    // Typed reads — no default (throws on miss)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public String getString(String key) {
        return loadOrThrow(key);
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(String key) {
        String value = loadOrThrow(key);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigValueConversionException(key, value, "int");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key) {
        String value = loadOrThrow(key);
        return parseBoolean(key, value);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBigDecimal(String key) {
        String value = loadOrThrow(key);
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigValueConversionException(key, value, "BigDecimal");
        }
    }

    // -------------------------------------------------------------------------
    // Typed reads — with default (never throws)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public String getString(String key, String defaultValue) {
        return loadOptional(key).orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        return loadOptional(key).map(v -> {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                log.warn("Cannot convert config value for key '{}' to int — using default {}", key, defaultValue);
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        return loadOptional(key).map(v -> parseBoolean(key, v)).orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return loadOptional(key).map(v -> {
            try {
                return new BigDecimal(v.trim());
            } catch (NumberFormatException e) {
                log.warn("Cannot convert config value for key '{}' to BigDecimal — using default", key);
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void set(String key, String value, String module, String description) {
        String normalisedKey = key.trim().toLowerCase();

        // Capture old value before overwrite (for audit log)
        String oldValue = loadOptional(normalisedKey).orElse(null);

        SystemConfig config = systemConfigRepository.findByConfigKey(normalisedKey)
                .orElseGet(() -> SystemConfig.builder()
                        .configKey(normalisedKey)
                        .module(module)
                        .build());

        config.setConfigValue(value);
        config.setModule(module);
        if (description != null) {
            config.setDescription(description);
        }

        systemConfigRepository.save(config);
        configCache.put(normalisedKey, value);

        log.info("Config key set: {}", normalisedKey);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CONFIG_UPDATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("SystemConfig")
                .resourceId(normalisedKey)
                .details(buildAuditDetails(normalisedKey, oldValue, value))
                .build());
    }

    @Override
    @Transactional
    public void set(String key, String value, String module) {
        set(key, value, module, null);
    }

    @Override
    @Transactional
    public void delete(String key) {
        String normalisedKey = key.trim().toLowerCase();

        systemConfigRepository.findByConfigKey(normalisedKey).ifPresent(config -> {
            systemConfigRepository.delete(config);
            configCache.evict(normalisedKey);
            log.info("Config key deleted: {}", normalisedKey);

            auditService.log(AuditEventRequest.builder()
                    .actionCode("CONFIG_DELETED")
                    .module(AttendAIConstants.MODULE_CORE)
                    .resourceType("SystemConfig")
                    .resourceId(normalisedKey)
                    .details("{\"key\":\"" + normalisedKey + "\"}")
                    .build());
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a key from cache first, then DB.
     * Throws {@link ConfigKeyNotFoundException} when not found.
     */
    private String loadOrThrow(String key) {
        return loadOptional(key).orElseThrow(() -> {
            log.warn("Config key not found and no default provided: {}", key);
            return new ConfigKeyNotFoundException(key);
        });
    }

    /**
     * Resolves a key from cache first, then DB. Returns empty on miss.
     */
    private Optional<String> loadOptional(String key) {
        String normalisedKey = key.trim().toLowerCase();

        // 1. Cache hit
        Optional<String> cached = configCache.get(normalisedKey);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. DB lookup
        Optional<String> fromDb = systemConfigRepository
                .findByConfigKey(normalisedKey)
                .map(SystemConfig::getConfigValue);

        // 3. Populate cache on DB hit
        fromDb.ifPresent(value -> configCache.put(normalisedKey, value));

        return fromDb;
    }

    private boolean parseBoolean(String key, String value) {
        String trimmed = value.trim().toLowerCase();
        if ("true".equals(trimmed)) return true;
        if ("false".equals(trimmed)) return false;
        throw new ConfigValueConversionException(key, value, "boolean");
    }

    /**
     * Builds the audit details JSON, masking values for keys that appear sensitive.
     */
    private String buildAuditDetails(String key, String oldValue, String newValue) {
        boolean sensitive = SENSITIVE_KEY_FRAGMENTS.stream()
                .anyMatch(fragment -> key.toLowerCase().contains(fragment));

        String displayOld = sensitive ? "[REDACTED]" : oldValue;
        String displayNew = sensitive ? "[REDACTED]" : newValue;

        return "{\"key\":\"" + key + "\","
                + "\"old_value\":" + jsonString(displayOld) + ","
                + "\"new_value\":" + jsonString(displayNew) + "}";
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
