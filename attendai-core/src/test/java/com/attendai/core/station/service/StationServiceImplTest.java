package com.attendai.core.station.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.config.service.ConfigService;
import com.attendai.core.station.dto.ChangeStationStatusRequest;
import com.attendai.core.station.dto.CreateStationRequest;
import com.attendai.core.station.dto.StationRegistrationResponse;
import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import com.attendai.core.station.exception.StationAlreadyExistsException;
import com.attendai.core.station.exception.StationNotFoundException;
import com.attendai.core.station.mapper.StationMapper;
import com.attendai.core.station.repository.StationConfigRepository;
import com.attendai.core.station.repository.StationRepository;
import com.attendai.core.station.util.StationKeyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceImplTest {

    @Mock StationRepository       stationRepository;
    @Mock StationConfigRepository stationConfigRepository;
    @Mock StationMapper            stationMapper;
    @Mock ConfigService            configService;
    @Mock AuditService             auditService;

    private StationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StationServiceImpl(stationRepository, stationConfigRepository,
                stationMapper, configService, auditService);
    }

    // -------------------------------------------------------------------------
    // createStation
    // -------------------------------------------------------------------------

    @Test
    void createStation_shouldReturnRegistrationResponse_withRawApiKey() {
        when(stationRepository.existsByName("Gate 1")).thenReturn(false);
        when(stationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateStationRequest req = new CreateStationRequest();
        req.setName("Gate 1");
        req.setType(StationType.ENTRY_EXIT);

        StationRegistrationResponse result = service.createStation(req);

        assertThat(result.getApiKey()).isNotBlank().startsWith("ak_");
        assertThat(result.getName()).isEqualTo("Gate 1");
        verify(stationRepository).save(any(Station.class));
        verify(auditService).log(any());
    }

    @Test
    void createStation_shouldThrow409_whenNameAlreadyExists() {
        when(stationRepository.existsByName("Gate 1")).thenReturn(true);

        CreateStationRequest req = new CreateStationRequest();
        req.setName("Gate 1");
        req.setType(StationType.ENTRY);

        assertThatThrownBy(() -> service.createStation(req))
                .isInstanceOf(StationAlreadyExistsException.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    void createStation_shouldStoreHashNotRawKey() {
        when(stationRepository.existsByName("Gate 2")).thenReturn(false);

        Station[] capturedStation = new Station[1];
        when(stationRepository.save(any())).thenAnswer(inv -> {
            capturedStation[0] = inv.getArgument(0);
            return capturedStation[0];
        });

        CreateStationRequest req = new CreateStationRequest();
        req.setName("Gate 2");
        req.setType(StationType.EXIT);

        StationRegistrationResponse result = service.createStation(req);

        // The hash stored in the entity must NOT equal the raw key
        assertThat(capturedStation[0].getApiKeyHash())
                .isNotEqualTo(result.getApiKey())
                .hasSize(64); // SHA-256 hex is always 64 chars
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(StationNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldTransition_whenValid() {
        Station station = buildStation(StationStatus.ACTIVE);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any())).thenReturn(station);

        ChangeStationStatusRequest req = new ChangeStationStatusRequest();
        req.setStatus(StationStatus.MAINTENANCE);

        service.changeStatus(1L, req);

        assertThat(station.getStatus()).isEqualTo(StationStatus.MAINTENANCE);
        verify(auditService).log(any());
    }

    @Test
    void changeStatus_shouldThrow400_whenInvalidTransition() {
        // INACTIVE → MAINTENANCE is not allowed
        Station station = buildStation(StationStatus.INACTIVE);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        ChangeStationStatusRequest req = new ChangeStationStatusRequest();
        req.setStatus(StationStatus.MAINTENANCE);

        assertThatThrownBy(() -> service.changeStatus(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void changeStatus_allValidTransitions_shouldSucceed() {
        // ACTIVE → INACTIVE
        verifyTransition(StationStatus.ACTIVE, StationStatus.INACTIVE);
        // ACTIVE → MAINTENANCE
        verifyTransition(StationStatus.ACTIVE, StationStatus.MAINTENANCE);
        // INACTIVE → ACTIVE
        verifyTransition(StationStatus.INACTIVE, StationStatus.ACTIVE);
        // MAINTENANCE → ACTIVE
        verifyTransition(StationStatus.MAINTENANCE, StationStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // regenerateApiKey
    // -------------------------------------------------------------------------

    @Test
    void regenerateApiKey_shouldReturnNewRawKey_andUpdateHash() {
        Station station = buildStation(StationStatus.ACTIVE);
        String originalHash = station.getApiKeyHash();

        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any())).thenReturn(station);

        StationRegistrationResponse result = service.regenerateApiKey(1L);

        assertThat(result.getApiKey()).isNotBlank().startsWith("ak_");
        assertThat(station.getApiKeyHash())
                .isNotEqualTo(originalHash)
                .isNotEqualTo(result.getApiKey())
                .hasSize(64);
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // heartbeat
    // -------------------------------------------------------------------------

    @Test
    void recordHeartbeat_shouldCallRepository() {
        service.recordHeartbeat(5L);
        verify(stationRepository).updateLastSeenAt(anyLong(), any());
    }

    // -------------------------------------------------------------------------
    // isActiveById
    // -------------------------------------------------------------------------

    @Test
    void isActiveById_shouldReturnTrue_whenStationIsActive() {
        when(stationRepository.findById(1L))
                .thenReturn(Optional.of(buildStation(StationStatus.ACTIVE)));
        assertThat(service.isActiveById(1L)).isTrue();
    }

    @Test
    void isActiveById_shouldReturnFalse_whenInactive() {
        when(stationRepository.findById(1L))
                .thenReturn(Optional.of(buildStation(StationStatus.INACTIVE)));
        assertThat(service.isActiveById(1L)).isFalse();
    }

    @Test
    void isActiveById_shouldReturnFalse_whenNotFound() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.isActiveById(99L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // findByApiKeyHash
    // -------------------------------------------------------------------------

    @Test
    void findByApiKeyHash_shouldReturnStation_whenHashMatches() {
        Station station = buildStation(StationStatus.ACTIVE);
        when(stationRepository.findByApiKeyHash("abc123")).thenReturn(Optional.of(station));

        assertThat(service.findByApiKeyHash("abc123")).isPresent();
    }

    // -------------------------------------------------------------------------
    // getEffectiveConfig
    // -------------------------------------------------------------------------

    @Test
    void getEffectiveConfig_shouldMergeSystemDefaultsWithStationOverrides() {
        Station station = buildStation(StationStatus.ACTIVE);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(configService.getString("face.recognition.threshold", null)).thenReturn("0.85");
        when(configService.getString("face.liveness.enabled", null)).thenReturn("false");
        when(configService.getString("attendance.dedup.window-seconds", null)).thenReturn("300");

        com.attendai.core.station.entity.StationConfig override =
                com.attendai.core.station.entity.StationConfig.builder()
                        .stationId(1L)
                        .configKey("face.recognition.threshold")
                        .configValue("0.95")
                        .build();
        when(stationConfigRepository.findByStationId(1L)).thenReturn(List.of(override));

        java.util.Map<String, String> effective = service.getEffectiveConfig(1L);

        // Station override (0.95) beats system default (0.85)
        assertThat(effective).containsEntry("face.recognition.threshold", "0.95");
        // System defaults present for keys without override
        assertThat(effective).containsEntry("face.liveness.enabled", "false");
    }

    // -------------------------------------------------------------------------
    // API key utilities
    // -------------------------------------------------------------------------

    @Test
    void generateRawApiKey_shouldProduceUniqueKeys() {
        String k1 = StationKeyUtils.generateRawApiKey();
        String k2 = StationKeyUtils.generateRawApiKey();
        assertThat(k1).isNotEqualTo(k2);
        assertThat(k1).startsWith("ak_");
    }

    @Test
    void sha256Hex_shouldProduceDeterministicHash() {
        String h1 = StationKeyUtils.sha256Hex("test-key");
        String h2 = StationKeyUtils.sha256Hex("test-key");
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    void sha256Hex_shouldProduceDifferentHashesForDifferentInputs() {
        assertThat(StationKeyUtils.sha256Hex("a")).isNotEqualTo(StationKeyUtils.sha256Hex("b"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void verifyTransition(StationStatus from, StationStatus to) {
        Station s = buildStation(from);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(s));
        when(stationRepository.save(any())).thenReturn(s);

        ChangeStationStatusRequest req = new ChangeStationStatusRequest();
        req.setStatus(to);
        service.changeStatus(1L, req);

        assertThat(s.getStatus()).isEqualTo(to);
    }

    private Station buildStation(StationStatus status) {
        return Station.builder()
                .name("Test Station")
                .type(StationType.ENTRY_EXIT)
                .status(status)
                .apiKeyHash(StationKeyUtils.sha256Hex("raw-test-key"))
                .build();
    }
}
