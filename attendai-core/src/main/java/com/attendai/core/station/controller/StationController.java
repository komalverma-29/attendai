package com.attendai.core.station.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.station.dto.ChangeStationStatusRequest;
import com.attendai.core.station.dto.CreateStationRequest;
import com.attendai.core.station.dto.StationConfigRequest;
import com.attendai.core.station.dto.StationRegistrationResponse;
import com.attendai.core.station.dto.StationResponse;
import com.attendai.core.station.dto.StationSummaryResponse;
import com.attendai.core.station.dto.UpdateStationRequest;
import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import com.attendai.core.station.service.StationService;
import com.attendai.core.station.service.StationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST controller for station management.
 *
 * Base path: /api/v1/core/stations
 *
 * Most endpoints require a user JWT with the appropriate CORE_STATION_* permission.
 * The heartbeat endpoint is authenticated by the station's own API key via
 * {@link com.attendai.core.station.filter.StationAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/core/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    /** POST /api/v1/core/stations — Register a new station. */
    @PostMapping
    @PreAuthorize("hasAuthority('CORE_STATION_CREATE')")
    public ResponseEntity<ApiResponse<StationRegistrationResponse>> createStation(
            @Valid @RequestBody CreateStationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(stationService.createStation(request)));
    }

    /** GET /api/v1/core/stations/{id} — Get station by ID (no API key field). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_STATION_READ')")
    public ResponseEntity<ApiResponse<StationResponse>> getStation(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(stationService.findById(id)));
    }

    /** GET /api/v1/core/stations — List stations with optional filters. */
    @GetMapping
    @PreAuthorize("hasAuthority('CORE_STATION_READ')")
    public ResponseEntity<PageResponse<StationSummaryResponse>> listStations(
            @RequestParam(name = "status", required = false) StationStatus status,
            @RequestParam(name = "type",   required = false) StationType   type,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(
                PageResponse.of(stationService.listStations(status, type, pageParams.toPageable())));
    }

    /** PUT /api/v1/core/stations/{id} — Update station details. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_STATION_UPDATE')")
    public ResponseEntity<ApiResponse<StationResponse>> updateStation(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateStationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stationService.updateStation(id, request)));
    }

    /** PATCH /api/v1/core/stations/{id}/status — Change station status. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CORE_STATION_UPDATE')")
    public ResponseEntity<ApiResponse<StationResponse>> changeStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeStationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stationService.changeStatus(id, request)));
    }

    /** POST /api/v1/core/stations/{id}/regenerate-key — Issue a new API key. */
    @PostMapping("/{id}/regenerate-key")
    @PreAuthorize("hasAuthority('CORE_STATION_UPDATE')")
    public ResponseEntity<ApiResponse<StationRegistrationResponse>> regenerateKey(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(stationService.regenerateApiKey(id)));
    }

    /** DELETE /api/v1/core/stations/{id} — Soft-delete a station. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_STATION_DELETE')")
    public ResponseEntity<Void> deleteStation(@PathVariable("id") Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/core/stations/heartbeat
     *
     * Authenticated via the station's X-Station-Api-Key header (not a user JWT).
     * The StationAuthenticationFilter sets the station ID as the security principal.
     * This endpoint is listed in PUBLIC_ENDPOINTS in SecurityConfig (JWT not required),
     * but the station filter must have authenticated the request.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Map<String, String>>> heartbeat() {
        Long stationId = SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Valid station API key required"));

        stationService.recordHeartbeat(stationId);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("timestamp", LocalDateTime.now().toString())));
    }

    /** GET /api/v1/core/stations/{id}/config — Get effective station configuration. */
    @GetMapping("/{id}/config")
    @PreAuthorize("hasAuthority('CORE_STATION_READ')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig(
            @PathVariable("id") Long id) {
        Map<String, String> config = stationService.getEffectiveConfig(id);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("stationId", id, "config", config)));
    }

    /** PUT /api/v1/core/stations/{id}/config — Set a station-specific config override. */
    @PutMapping("/{id}/config")
    @PreAuthorize("hasAuthority('CORE_STATION_UPDATE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setConfig(
            @PathVariable("id") Long id,
            @Valid @RequestBody StationConfigRequest request) {
        stationService.setConfigOverride(id, request.getKey(), request.getValue());
        Map<String, String> updated = stationService.getEffectiveConfig(id);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("stationId", id, "config", updated)));
    }
}
