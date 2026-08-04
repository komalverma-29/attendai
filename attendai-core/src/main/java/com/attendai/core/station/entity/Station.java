package com.attendai.core.station.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Attendance station entity.
 *
 * <p>The raw API key is NEVER stored. Only the SHA-256 hex hash is persisted
 * in {@code apiKeyHash}. The raw key is returned to the caller exactly once
 * at creation or key regeneration, then discarded.
 *
 * <p>Stations are domain-agnostic. Business modules link their own domain
 * entities to a station ID but Core never references those entities.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stations")
public class Station extends SoftDeletableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private StationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StationStatus status = StationStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * SHA-256 hex hash of the raw API key.
     * Used as the lookup key for station authentication.
     * Never exposed in any API response.
     */
    @Column(name = "api_key_hash", nullable = false, unique = true, length = 64)
    private String apiKeyHash;

    /** Timestamp of the most recent heartbeat from this station. */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}
