package com.attendai.core.config.service;

import java.math.BigDecimal;

/**
 * Platform-wide system configuration service.
 *
 * <p>Provides typed access to runtime-configurable key-value settings.
 * All reads are served from the in-memory {@link com.attendai.core.config.cache.ConfigCache}
 * with automatic DB fallback on a cache miss.
 *
 * <p>All modules — Core and business — use this service to read operational parameters
 * at runtime without redeployment.
 *
 * <p>SECURITY: Config values must never contain secrets, tokens, or passwords.
 * Use environment variables for sensitive configuration.
 */
public interface ConfigService {

    // -------------------------------------------------------------------------
    // Typed read — throws ConfigKeyNotFoundException if key absent and no default
    // -------------------------------------------------------------------------

    String     getString(String key);
    int        getInt(String key);
    boolean    getBoolean(String key);
    BigDecimal getBigDecimal(String key);

    // -------------------------------------------------------------------------
    // Typed read with default — never throws
    // -------------------------------------------------------------------------

    String     getString(String key, String defaultValue);
    int        getInt(String key, int defaultValue);
    boolean    getBoolean(String key, boolean defaultValue);
    BigDecimal getBigDecimal(String key, BigDecimal defaultValue);

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates or updates a configuration key.
     *
     * <p>If the key already exists it is overwritten (upsert semantics).
     * The old value is captured and included in the audit log before being
     * replaced. The cache is invalidated immediately after the DB write.
     *
     * @param key         the configuration key (will be lowercased)
     * @param value       the new value (max 1000 characters)
     * @param module      the owning module namespace
     * @param description optional human-readable description
     */
    void set(String key, String value, String module, String description);

    /**
     * Convenience overload without a description update.
     */
    void set(String key, String value, String module);

    /**
     * Removes a configuration key from both the database and the cache.
     * After deletion, reads for this key will return defaults or throw.
     *
     * @param key the key to delete
     */
    void delete(String key);
}
