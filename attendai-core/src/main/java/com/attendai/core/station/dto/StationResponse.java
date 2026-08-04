package com.attendai.core.station.dto;

import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full station response.
 * The {@code apiKeyHash} is intentionally absent — the hash must never leave the server.
 * The raw API key was returned only at creation time.
 */
@Getter
@Builder
public class StationResponse {
    private final Long          id;
    private final String        name;
    private final StationType   type;
    private final StationStatus status;
    private final String        description;
    private final String        locationName;
    private final BigDecimal    latitude;
    private final BigDecimal    longitude;
    private final LocalDateTime lastSeenAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
