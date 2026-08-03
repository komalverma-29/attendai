package com.attendai.core.config.entity;

import com.attendai.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single runtime-configurable key-value entry.
 *
 * <p>Keys are stored in lowercase and follow the {@code <module>.<category>.<name>}
 * naming convention. Values are always stored as strings; typed access is provided
 * by {@link com.attendai.core.config.service.ConfigService}.
 *
 * <p>SECURITY: Config values must never contain secrets, passwords, or tokens.
 * Use environment variables for sensitive configuration.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_configs")
public class SystemConfig extends BaseEntity {

    /** Unique configuration key, stored in lowercase. Max 200 characters. */
    @Column(name = "config_key", nullable = false, unique = true, length = 200)
    private String configKey;

    /** Configuration value stored as a string. Max 1000 characters. */
    @Column(name = "config_value", nullable = false, length = 1000)
    private String configValue;

    /** Module namespace, e.g. "attendance", "face", "auth". */
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    /** Human-readable description of what this key controls. */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Reserved for future encrypted-value support.
     * Always {@code false} in V1 — config values must not be secrets.
     */
    @Column(name = "is_encrypted", nullable = false)
    @Builder.Default
    private boolean isEncrypted = false;
}
