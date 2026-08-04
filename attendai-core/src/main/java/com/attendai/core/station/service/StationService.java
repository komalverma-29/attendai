package com.attendai.core.station.service;

import com.attendai.core.station.dto.ChangeStationStatusRequest;
import com.attendai.core.station.dto.CreateStationRequest;
import com.attendai.core.station.dto.StationRegistrationResponse;
import com.attendai.core.station.dto.StationResponse;
import com.attendai.core.station.dto.StationSummaryResponse;
import com.attendai.core.station.dto.UpdateStationRequest;
import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

/**
 * Station management service.
 *
 * Exposes both HTTP-facing operations and an internal API used by
 * {@code StationAuthenticationFilter} and {@code core-attendance}.
 */
public interface StationService {

    // -------------------------------------------------------------------------
    // HTTP-facing operations
    // -------------------------------------------------------------------------

    StationRegistrationResponse createStation(CreateStationRequest request);

    StationResponse findById(Long id);

    Page<StationSummaryResponse> listStations(StationStatus status, StationType type, Pageable pageable);

    StationResponse updateStation(Long id, UpdateStationRequest request);

    StationResponse changeStatus(Long id, ChangeStationStatusRequest request);

    StationRegistrationResponse regenerateApiKey(Long id);

    void deleteStation(Long id);

    void recordHeartbeat(Long stationId);

    Map<String, String> getEffectiveConfig(Long stationId);

    void setConfigOverride(Long stationId, String key, String value);

    // -------------------------------------------------------------------------
    // Internal API (consumed by StationAuthenticationFilter and core-attendance)
    // -------------------------------------------------------------------------

    /** Finds a station by its SHA-256 API key hash. Used by the auth filter. */
    Optional<Station> findByApiKeyHash(String hash);

    /** Returns true if a station exists and is not soft-deleted. */
    boolean existsById(Long id);

    /** Returns true if the station exists, is not deleted, and has ACTIVE status. */
    boolean isActiveById(Long id);
}
