package com.attendai.core.station.filter;

import com.attendai.core.station.entity.Station;
import com.attendai.core.station.entity.StationStatus;
import com.attendai.core.station.service.StationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.attendai.core.station.util.StationKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Intercepts requests carrying the {@code X-Station-Api-Key} header and authenticates
 * the station against the hashed key stored in the database.
 *
 * <p>On success: a {@link UsernamePasswordAuthenticationToken} is placed in the
 * {@link SecurityContextHolder} with:
 * <ul>
 *   <li>Principal: the station's surrogate Long ID</li>
 *   <li>Authorities: {@code ROLE_STATION}, {@code CORE_FACE_RECOGNIZE},
 *       {@code CORE_ATTENDANCE_RECORD}</li>
 * </ul>
 *
 * <p>On failure (missing/invalid/revoked key, or non-ACTIVE station):
 * the filter does NOT set authentication — the request proceeds unauthenticated
 * and Spring Security's configured {@code AuthenticationEntryPoint} returns 401.
 *
 * <p>This filter runs before {@link com.attendai.core.auth.filter.JwtAuthenticationFilter}
 * in the Spring Security filter chain. A request may carry either a station key
 * or a user JWT — never both.
 */
@Slf4j
@RequiredArgsConstructor
public class StationAuthenticationFilter extends OncePerRequestFilter {

    static final String STATION_API_KEY_HEADER = "X-Station-Api-Key";

    /** Authorities granted to all authenticated stations. */
    private static final List<SimpleGrantedAuthority> STATION_AUTHORITIES = List.of(
            new SimpleGrantedAuthority("ROLE_STATION"),
            new SimpleGrantedAuthority("CORE_FACE_RECOGNIZE"),
            new SimpleGrantedAuthority("CORE_ATTENDANCE_RECORD")
    );

    private final StationService stationService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        String rawKey = request.getHeader(STATION_API_KEY_HEADER);

        if (StringUtils.hasText(rawKey)) {
            authenticateStation(rawKey);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateStation(String rawKey) {
        try {
            String hash = StationKeyUtils.sha256Hex(rawKey);

            stationService.findByApiKeyHash(hash).ifPresent(station -> {
                if (!isUsable(station)) {
                    log.warn("Station authentication rejected — status={} stationId={}",
                            station.getStatus(), station.getId());
                    return;
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                station.getId(), null, STATION_AUTHORITIES);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Station authenticated | stationId={}", station.getId());
            });
        } catch (Exception e) {
            log.warn("Unexpected error during station authentication: {}", e.getMessage());
            // Do not propagate — let SecurityContext remain empty
        }
    }

    /**
     * A station is usable for authentication if it is not deleted and not INACTIVE.
     * MAINTENANCE stations CAN authenticate (for heartbeats) but cannot submit events —
     * that constraint is enforced at the service/controller level.
     */
    private boolean isUsable(Station station) {
        return !station.isDeleted() && station.getStatus() != StationStatus.INACTIVE;
    }
}
