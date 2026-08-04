package com.attendai.core.station.dto;

import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response returned ONLY at station creation or API key regeneration.
 *
 * Contains the raw API key — the ONLY time the plaintext key is visible.
 * It is not stored and cannot be retrieved again.
 */
@Getter
@Builder
public class StationRegistrationResponse {
    private final Long          id;
    private final String        name;
    private final StationType   type;
    private final StationStatus status;
    /** Raw API key — shown exactly once. Store it immediately. */
    private final String        apiKey;
    private final LocalDateTime createdAt;
}
