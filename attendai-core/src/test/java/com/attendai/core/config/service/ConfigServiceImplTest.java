package com.attendai.core.config.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.config.cache.ConfigCache;
import com.attendai.core.config.entity.SystemConfig;
import com.attendai.core.config.exception.ConfigKeyNotFoundException;
import com.attendai.core.config.exception.ConfigValueConversionException;
import com.attendai.core.config.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceImplTest {

    @Mock SystemConfigRepository systemConfigRepository;
    @Mock AuditService           auditService;

    private ConfigCache        configCache;
    private ConfigServiceImpl  configService;

    @BeforeEach
    void setUp() {
        configCache   = new ConfigCache();
        configService = new ConfigServiceImpl(systemConfigRepository, configCache, auditService);
    }

    // -------------------------------------------------------------------------
    // getString — throws on miss
    // -------------------------------------------------------------------------

    @Test
    void getString_shouldReturnValue_fromCache() {
        configCache.put("some.key", "cachedValue");

        String result = configService.getString("some.key");

        assertThat(result).isEqualTo("cachedValue");
        verify(systemConfigRepository, never()).findByConfigKey(anyString());
    }

    @Test
    void getString_shouldLoadFromDb_onCacheMiss() {
        when(systemConfigRepository.findByConfigKey("some.key"))
                .thenReturn(Optional.of(config("some.key", "dbValue")));

        String result = configService.getString("some.key");

        assertThat(result).isEqualTo("dbValue");
    }

    @Test
    void getString_shouldPopulateCache_afterDbLoad() {
        when(systemConfigRepository.findByConfigKey("some.key"))
                .thenReturn(Optional.of(config("some.key", "dbValue")));

        configService.getString("some.key");

        // Second call should hit cache, not DB
        configService.getString("some.key");
        verify(systemConfigRepository).findByConfigKey("some.key"); // called only once
    }

    @Test
    void getString_shouldThrow_whenKeyNotFound() {
        when(systemConfigRepository.findByConfigKey("missing.key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getString("missing.key"))
                .isInstanceOf(ConfigKeyNotFoundException.class)
                .hasMessageContaining("missing.key");
    }

    // -------------------------------------------------------------------------
    // getString — with default (never throws)
    // -------------------------------------------------------------------------

    @Test
    void getString_withDefault_shouldReturnDefault_whenKeyAbsent() {
        when(systemConfigRepository.findByConfigKey("absent.key")).thenReturn(Optional.empty());

        String result = configService.getString("absent.key", "fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void getString_withDefault_shouldReturnStoredValue_whenKeyPresent() {
        configCache.put("present.key", "storedValue");

        String result = configService.getString("present.key", "fallback");

        assertThat(result).isEqualTo("storedValue");
    }

    // -------------------------------------------------------------------------
    // getInt
    // -------------------------------------------------------------------------

    @Test
    void getInt_shouldParseInteger_whenValueIsNumeric() {
        configCache.put("int.key", "42");
        assertThat(configService.getInt("int.key")).isEqualTo(42);
    }

    @Test
    void getInt_shouldThrowConversionException_whenValueIsNotNumeric() {
        configCache.put("bad.int", "not-a-number");

        assertThatThrownBy(() -> configService.getInt("bad.int"))
                .isInstanceOf(ConfigValueConversionException.class);
    }

    @Test
    void getInt_withDefault_shouldReturnDefault_whenKeyAbsent() {
        when(systemConfigRepository.findByConfigKey("absent.int")).thenReturn(Optional.empty());
        assertThat(configService.getInt("absent.int", 99)).isEqualTo(99);
    }

    // -------------------------------------------------------------------------
    // getBoolean
    // -------------------------------------------------------------------------

    @Test
    void getBoolean_shouldReturnTrue_forTrueValue() {
        configCache.put("bool.true", "true");
        assertThat(configService.getBoolean("bool.true")).isTrue();
    }

    @Test
    void getBoolean_shouldReturnFalse_forFalseValue() {
        configCache.put("bool.false", "false");
        assertThat(configService.getBoolean("bool.false")).isFalse();
    }

    @Test
    void getBoolean_shouldBeCaseInsensitive() {
        configCache.put("bool.upper", "TRUE");
        assertThat(configService.getBoolean("bool.upper")).isTrue();
    }

    @Test
    void getBoolean_shouldThrowConversionException_forInvalidValue() {
        configCache.put("bad.bool", "yes");

        assertThatThrownBy(() -> configService.getBoolean("bad.bool"))
                .isInstanceOf(ConfigValueConversionException.class);
    }

    @Test
    void getBoolean_withDefault_shouldReturnDefault_whenAbsent() {
        when(systemConfigRepository.findByConfigKey("absent.bool")).thenReturn(Optional.empty());
        assertThat(configService.getBoolean("absent.bool", true)).isTrue();
    }

    // -------------------------------------------------------------------------
    // getBigDecimal
    // -------------------------------------------------------------------------

    @Test
    void getBigDecimal_shouldParse_whenValueIsNumeric() {
        configCache.put("decimal.key", "0.85");
        assertThat(configService.getBigDecimal("decimal.key")).isEqualByComparingTo("0.85");
    }

    @Test
    void getBigDecimal_shouldThrowConversionException_whenInvalid() {
        configCache.put("bad.decimal", "abc");

        assertThatThrownBy(() -> configService.getBigDecimal("bad.decimal"))
                .isInstanceOf(ConfigValueConversionException.class);
    }

    @Test
    void getBigDecimal_withDefault_shouldReturnDefault_whenAbsent() {
        when(systemConfigRepository.findByConfigKey("absent.decimal")).thenReturn(Optional.empty());
        BigDecimal def = new BigDecimal("1.0");
        assertThat(configService.getBigDecimal("absent.decimal", def)).isEqualByComparingTo("1.0");
    }

    // -------------------------------------------------------------------------
    // set — upsert and cache invalidation
    // -------------------------------------------------------------------------

    @Test
    void set_shouldSaveToDb_andUpdateCache() {
        when(systemConfigRepository.findByConfigKey("face.threshold")).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configService.set("face.threshold", "0.90", "face");

        verify(systemConfigRepository).save(any(SystemConfig.class));
        // After set, cache should have the new value
        assertThat(configCache.get("face.threshold")).isPresent().contains("0.90");
    }

    @Test
    void set_shouldUpsert_whenKeyAlreadyExists() {
        SystemConfig existing = config("face.threshold", "0.85");
        when(systemConfigRepository.findByConfigKey("face.threshold")).thenReturn(Optional.of(existing));
        when(systemConfigRepository.save(any())).thenReturn(existing);

        configService.set("face.threshold", "0.95", "face");

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getConfigValue()).isEqualTo("0.95");
    }

    @Test
    void set_shouldNormaliseKeyToLowercase() {
        when(systemConfigRepository.findByConfigKey("face.threshold")).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configService.set("FACE.THRESHOLD", "0.90", "face");

        // The key is normalised to lowercase before the DB call.
        // findByConfigKey is called at least once with the lowercase key.
        verify(systemConfigRepository, org.mockito.Mockito.atLeastOnce())
                .findByConfigKey("face.threshold");
        // The uppercase form must never be used
        verify(systemConfigRepository, org.mockito.Mockito.never())
                .findByConfigKey("FACE.THRESHOLD");
    }

    @Test
    void set_shouldWriteAuditLog() {
        when(systemConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configService.set("some.key", "value", "core");

        verify(auditService).log(any());
    }

    @Test
    void set_shouldMaskSensitiveKeyValues_inAuditLog() {
        when(systemConfigRepository.findByConfigKey("auth.secret.key")).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configService.set("auth.secret.key", "super-secret-value", "auth");

        ArgumentCaptor<com.attendai.core.audit.dto.AuditEventRequest> captor =
                ArgumentCaptor.forClass(com.attendai.core.audit.dto.AuditEventRequest.class);
        verify(auditService).log(captor.capture());

        String details = captor.getValue().getDetails();
        assertThat(details).contains("[REDACTED]");
        assertThat(details).doesNotContain("super-secret-value");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_shouldRemoveFromDbAndCache_whenKeyExists() {
        SystemConfig existing = config("some.key", "value");
        configCache.put("some.key", "value");
        when(systemConfigRepository.findByConfigKey("some.key")).thenReturn(Optional.of(existing));

        configService.delete("some.key");

        verify(systemConfigRepository).delete(existing);
        assertThat(configCache.get("some.key")).isEmpty();
        verify(auditService).log(any());
    }

    @Test
    void delete_shouldDoNothing_whenKeyDoesNotExist() {
        when(systemConfigRepository.findByConfigKey("missing.key")).thenReturn(Optional.empty());

        configService.delete("missing.key");

        verify(systemConfigRepository, never()).delete(any());
        verify(auditService, never()).log(any());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SystemConfig config(String key, String value) {
        return SystemConfig.builder().configKey(key).configValue(value).module("core").build();
    }
}
