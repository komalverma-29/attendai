package com.attendai.school.settings.entity;

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
 * A single school-scoped configuration override.
 *
 * <p>Settings are key-value pairs scoped to a specific school.
 * They override the platform-level defaults from {@code core-config} and the
 * in-code school module defaults defined in {@link SchoolSettingsService}.
 *
 * <p>No soft-delete on this entity — deleting a setting resets it to the default.
 * The table enforces UNIQUE (school_id, setting_key).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_settings")
public class SchoolSetting extends BaseEntity {

    /** FK → school_schools(id). Scopes this setting to a specific school. */
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    /** The setting key, e.g. {@code school.weekend.days}. Max 200 characters. */
    @Column(name = "setting_key", nullable = false, length = 200)
    private String settingKey;

    /** The overridden value. Max 1000 characters. */
    @Column(name = "setting_value", nullable = false, length = 1000)
    private String settingValue;

    /** Optional human-readable description of why this value was set. Max 500 characters. */
    @Column(name = "description", length = 500)
    private String description;
}
