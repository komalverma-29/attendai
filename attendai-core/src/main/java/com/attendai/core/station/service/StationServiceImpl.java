package com.attendai.core.station.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.config.service.ConfigService;
import com.attendai.core.station.dto.ChangeStationStatusRequest;
import com.attendai.core.station.dto.CreateStationRequest;
import com.attendai.core.station.dto.StationRegistrationResponse;
import com.attendai.core.station.dto.StationResponse;
import com.attendai.core.station.dto.StationSummaryResponse;
import com.attendai.core.station.dto.UpdateStationRequest;
import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationConfig;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import com.attendai.core.station.exception.StationAlreadyExistsException;
import com.attendai.core.station.exception.StationNotFoundException;
import com.attendai.core.station.mapper.StationMapper;
import com.attendai.core.station.repository.StationConfigRepository;
import com.attendai.core.station.repository.StationRepository;
import com.attendai.core.station.util.StationKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {

    /** Allowed status transitions: key = current, value = valid targets. */
    private static final Map<StationStatus, Set<StationStatus>> ALLOWED_TRANSITIONS = Map.of(
            StationStatus.ACTIVE,      EnumSet.of(StationStatus.INACTIVE, StationStatus.MAINTENANCE),
            StationStatus.INACTIVE,    EnumSet.of(StationStatus.ACTIVE),
            StationStatus.MAINTENANCE, EnumSet.of(StationStatus.ACTIVE)
    );

    private final StationRepository       stationRepository;
    private final StationConfigRepository stationConfigRepository;
    private final StationMapper           stationMapper;
    private final ConfigService           configService;
    private final AuditService            auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StationRegistrationResponse createStation(CreateStationRequest request) {
        if (stationRepository.existsByName(request.getName())) {
            throw new StationAlreadyExistsException(request.getName());
        }

        String rawKey  = generateRawApiKey();
        String keyHash = sha256Hex(rawKey);

        Station station = Station.builder()
                .name(request.getName())
                .type(request.getType())
                .status(StationStatus.ACTIVE)
                .description(request.getDescription())
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .apiKeyHash(keyHash)
                .build();

        Station saved = stationRepository.save(station);
        log.info("Station registered | stationId={} name={}", saved.getId(), saved.getName());

        auditService.log(AuditEventRequest.builder()
                .actionCode("STATION_CREATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Station")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"name\":\"" + saved.getName() + "\"}")
                .build());

        return StationRegistrationResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .type(saved.getType())
                .status(saved.getStatus())
                .apiKey(rawKey)           // Only time raw key is returned
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StationResponse findById(Long id) {
        return stationMapper.toResponse(requireStation(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StationSummaryResponse> listStations(StationStatus status, StationType type,
                                                      Pageable pageable) {
        return stationRepository.findByFilters(status, type, pageable)
                .map(stationMapper::toSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StationResponse updateStation(Long id, UpdateStationRequest request) {
        Station station = requireStation(id);

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equals(station.getName()) && stationRepository.existsByName(newName)) {
                throw new StationAlreadyExistsException(newName);
            }
            station.setName(newName);
        }
        if (request.getType()         != null) station.setType(request.getType());
        if (request.getDescription()  != null) station.setDescription(request.getDescription());
        if (request.getLocationName() != null) station.setLocationName(request.getLocationName());
        if (request.getLatitude()     != null) station.setLatitude(request.getLatitude());
        if (request.getLongitude()    != null) station.setLongitude(request.getLongitude());

        Station saved = stationRepository.save(station);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STATION_UPDATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Station")
                .resourceId(String.valueOf(id))
                .build());

        return stationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StationResponse changeStatus(Long id, ChangeStationStatusRequest request) {
        Station station = requireStation(id);
        StationStatus from = station.getStatus();
        StationStatus to   = request.getStatus();

        Set<StationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new ValidationException(
                    "Cannot transition station status from " + from + " to " + to);
        }

        station.setStatus(to);
        Station saved = stationRepository.save(station);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STATION_STATUS_CHANGED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Station")
                .resourceId(String.valueOf(id))
                .details("{\"from\":\"" + from + "\",\"to\":\"" + to + "\"}")
                .build());

        return stationMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // API key regeneration
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StationRegistrationResponse regenerateApiKey(Long id) {
        Station station = requireStation(id);

        String newRawKey  = generateRawApiKey();
        String newKeyHash = sha256Hex(newRawKey);

        station.setApiKeyHash(newKeyHash);
        stationRepository.save(station);

        log.info("Station API key regenerated | stationId={}", id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STATION_KEY_REGENERATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Station")
                .resourceId(String.valueOf(id))
                .build());

        return StationRegistrationResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .type(station.getType())
                .status(station.getStatus())
                .apiKey(newRawKey)
                .createdAt(station.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteStation(Long id) {
        Station station = requireStation(id);

        // Attendance events dependency guard is enforced at the attendance module level.
        // The attendance table references stations(id) via FK — attempting to delete a
        // station with events will result in a FK constraint violation, which is caught
        // here and re-thrown as a ValidationException for a cleaner API response.
        try {
            station.softDelete();
            stationRepository.save(station);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("constraint")) {
                throw new ValidationException(
                        "Station with id " + id + " has attendance events and cannot be deleted");
            }
            throw e;
        }

        auditService.log(AuditEventRequest.builder()
                .actionCode("STATION_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Station")
                .resourceId(String.valueOf(id))
                .build());
    }

    // -------------------------------------------------------------------------
    // Heartbeat
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void recordHeartbeat(Long stationId) {
        stationRepository.updateLastSeenAt(stationId, LocalDateTime.now());
        log.debug("Heartbeat recorded | stationId={}", stationId);
        // Heartbeat is not audit-logged per spec (INFO log only above)
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getEffectiveConfig(Long stationId) {
        requireStation(stationId);

        // Start with system-level defaults from core-config
        // (using common station-relevant keys as the base)
        Map<String, String> effective = new LinkedHashMap<>();
        addSystemDefault(effective, "face.recognition.threshold");
        addSystemDefault(effective, "face.liveness.enabled");
        addSystemDefault(effective, "attendance.dedup.window-seconds");

        // Station-specific overrides take precedence
        stationConfigRepository.findByStationId(stationId)
                .forEach(sc -> effective.put(sc.getConfigKey(), sc.getConfigValue()));

        return effective;
    }

    @Override
    @Transactional
    public void setConfigOverride(Long stationId, String key, String value) {
        requireStation(stationId);

        stationConfigRepository.findByStationIdAndConfigKey(stationId, key)
                .ifPresentOrElse(
                        existing -> {
                            existing.setConfigValue(value);
                            stationConfigRepository.save(existing);
                        },
                        () -> stationConfigRepository.save(StationConfig.builder()
                                .stationId(stationId)
                                .configKey(key)
                                .configValue(value)
                                .build())
                );
    }

    // -------------------------------------------------------------------------
    // Internal API
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<Station> findByApiKeyHash(String hash) {
        return stationRepository.findByApiKeyHash(hash);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return stationRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveById(Long id) {
        return stationRepository.findById(id)
                .map(s -> s.getStatus() == StationStatus.ACTIVE)
                .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Station requireStation(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException(id));
    }

    private void addSystemDefault(Map<String, String> map, String key) {
        String val = configService.getString(key, null);
        if (val != null) {
            map.put(key, val);
        }
    }

    /** Delegates to {@link StationKeyUtils#generateRawApiKey()}. */
    static String generateRawApiKey() {
        return StationKeyUtils.generateRawApiKey();
    }

    /** Delegates to {@link StationKeyUtils#sha256Hex(String)}. */
    public static String sha256Hex(String input) {
        return StationKeyUtils.sha256Hex(input);
    }
}
