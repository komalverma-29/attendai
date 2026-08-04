package com.attendai.core.auth.config;

import com.attendai.core.auth.filter.JwtAuthenticationFilter;
import com.attendai.core.auth.service.JwtService;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.ErrorResponse;
import com.attendai.core.station.filter.StationAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Spring Security configuration for the AttendAI platform.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Stateless — no HTTP session is created.</li>
 *   <li>CSRF disabled — standard for stateless JWT APIs.</li>
 *   <li>All endpoints require authentication except the explicit public list.</li>
 *   <li>CORS origins configured from {@link SecurityProperties}.</li>
 *   <li>Both AuthenticationEntryPoint and AccessDeniedHandler return structured
 *       {@link ApiResponse} JSON responses.</li>
 * </ul>
 *
 * <p>Station authentication is handled by
 * {@link com.attendai.core.station.config.StationSecurityConfig}, which registers
 * {@link com.attendai.core.station.filter.StationAuthenticationFilter} separately.
 * This keeps SecurityConfig free of a dependency on the station module.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password-reset/request",
            "/api/v1/auth/password-reset/confirm",
            "/api/v1/core/stations/heartbeat",   // authenticated via X-Station-Api-Key
            "/actuator/health"
    };

    private final JwtService         jwtService;
    private final SecurityProperties securityProperties;
    private final ObjectMapper        objectMapper;

    /**
     * Optionally injected. Present at runtime when the station module is active.
     * Null in @WebMvcTest slices that don't include StationSecurityConfig.
     */
    @Autowired(required = false)
    private StationAuthenticationFilter stationAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var chain = http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED", "Authentication is required to access this resource"))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN", "You do not have permission to access this resource"))
            );

        // Register station filter before JWT filter when the station module is present
        if (stationAuthenticationFilter != null) {
            chain.addFilterBefore(stationAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        chain.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        int strength = Math.max(securityProperties.getBcryptStrength(), 10);
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(securityProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Station-Api-Key"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeErrorResponse(HttpServletResponse response, int status,
                                     String code, String message) {
        try {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ErrorResponse error = ErrorResponse.builder().code(code).message(message).build();
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(error)));
        } catch (Exception e) {
            response.setStatus(status);
        }
    }
}
