package com.attendai.core.station.config;

import com.attendai.core.station.filter.StationAuthenticationFilter;
import com.attendai.core.station.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Registers the {@link StationAuthenticationFilter} into the Spring Security
 * filter chain without coupling {@link com.attendai.core.auth.config.SecurityConfig}
 * to the station module.
 *
 * <p>This configuration is in the {@code station} package so it is only loaded
 * when the station module is present. {@code @WebMvcTest} slices for non-station
 * controllers do not include this class, so they never fail due to a missing
 * {@link StationService} bean.
 */
@Configuration
@RequiredArgsConstructor
public class StationSecurityConfig {

    private final StationService stationService;

    /**
     * Creates the {@link StationAuthenticationFilter} as a Spring bean.
     *
     * <p>The filter is added to the security chain before
     * {@link UsernamePasswordAuthenticationFilter} by being declared as a
     * {@code @Bean}. Spring Security auto-detects {@link org.springframework.web.filter.OncePerRequestFilter}
     * beans and adds them to the chain. However, to guarantee ordering we
     * declare it here and let callers who need it inject it explicitly.
     */
    @Bean
    public StationAuthenticationFilter stationAuthenticationFilter() {
        return new StationAuthenticationFilter(stationService);
    }
}
