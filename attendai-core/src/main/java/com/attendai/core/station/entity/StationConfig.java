package com.attendai.core.station.entity;

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
 * Per-station configuration override.
 *
 * <p>Station-specific overrides take precedence over system-level defaults
 * from {@code core-config} when computing the effective configuration for a station.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "station_configs")
public class StationConfig extends BaseEntity {

    @Column(name = "station_id", nullable = false, updatable = false)
    private Long stationId;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 1000)
    private String configValue;
}
