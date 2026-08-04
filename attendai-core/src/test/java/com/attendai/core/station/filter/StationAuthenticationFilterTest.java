package com.attendai.core.station.filter;

import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.entity.StationType;
import com.attendai.core.station.service.StationService;
import com.attendai.core.station.util.StationKeyUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationAuthenticationFilterTest {

    @Mock StationService      stationService;
    @Mock HttpServletRequest  request;
    @Mock HttpServletResponse response;
    @Mock FilterChain         filterChain;

    private StationAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new StationAuthenticationFilter(stationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_shouldSetAuthentication_whenValidActiveStationKey() throws Exception {
        String rawKey = "ak_validkey";
        String hash   = StationKeyUtils.sha256Hex(rawKey);

        Station station = buildStation(1L, StationStatus.ACTIVE);
        when(request.getHeader("X-Station-Api-Key")).thenReturn(rawKey);
        when(stationService.findByApiKeyHash(hash)).thenReturn(Optional.of(station));

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_STATION", "CORE_FACE_RECOGNIZE", "CORE_ATTENDANCE_RECORD");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filter_shouldNotSetAuthentication_whenNoHeader() throws Exception {
        when(request.getHeader("X-Station-Api-Key")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filter_shouldNotSetAuthentication_whenKeyHashNotFound() throws Exception {
        String rawKey = "ak_unknownkey";
        String hash   = StationKeyUtils.sha256Hex(rawKey);

        when(request.getHeader("X-Station-Api-Key")).thenReturn(rawKey);
        when(stationService.findByApiKeyHash(hash)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filter_shouldNotSetAuthentication_whenStationIsInactive() throws Exception {
        String rawKey = "ak_inactivekey";
        String hash   = StationKeyUtils.sha256Hex(rawKey);

        Station station = buildStation(2L, StationStatus.INACTIVE);
        when(request.getHeader("X-Station-Api-Key")).thenReturn(rawKey);
        when(stationService.findByApiKeyHash(hash)).thenReturn(Optional.of(station));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filter_shouldSetAuthentication_whenStationIsInMaintenance() throws Exception {
        // MAINTENANCE stations CAN authenticate (for heartbeats)
        String rawKey = "ak_maintenancekey";
        String hash   = StationKeyUtils.sha256Hex(rawKey);

        Station station = buildStation(3L, StationStatus.MAINTENANCE);
        when(request.getHeader("X-Station-Api-Key")).thenReturn(rawKey);
        when(stationService.findByApiKeyHash(hash)).thenReturn(Optional.of(station));

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(3L);
    }

    @Test
    void filter_shouldContinueChain_evenIfExceptionThrown() throws Exception {
        when(request.getHeader("X-Station-Api-Key")).thenReturn("ak_badkey");
        when(stationService.findByApiKeyHash(any())).thenThrow(new RuntimeException("DB down"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private Station buildStation(Long id, StationStatus status) {
        Station station = Station.builder()
                .name("Test Station")
                .type(StationType.ENTRY_EXIT)
                .status(status)
                .apiKeyHash("hash")
                .build();
        station.setId(id);  // set the inherited id field from BaseEntity
        return station;
    }

    private String any() { return org.mockito.ArgumentMatchers.anyString(); }
}
