package com.attendai.core.auth.filter;

import com.attendai.core.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * Intercepts every incoming HTTP request and validates the JWT Bearer token.
 *
 * <p>On success: populates the Spring Security {@link SecurityContextHolder}
 * with a {@link UsernamePasswordAuthenticationToken} whose principal is
 * the user's Long ID and whose authorities are the permission codes from the token.
 *
 * <p>On failure (missing/invalid/expired token): does NOT throw. The filter
 * simply leaves the SecurityContext empty. Spring Security's configured
 * {@link org.springframework.security.web.AuthenticationEntryPoint} handles
 * the resulting 401 response for secured endpoints.
 *
 * <p>No database call is made during token validation — JWT is stateless.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                jwtService.validateToken(token).ifPresent(claims -> authenticate(claims));
            } catch (Exception e) {
                log.warn("Unexpected error during token validation: {}", e.getMessage());
                // Do not propagate — let SecurityContext remain empty and chain continue
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims) {
        try {
            Long userId = jwtService.extractUserId(claims);
            List<SimpleGrantedAuthority> authorities = jwtService.extractPermissions(claims)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            log.warn("Failed to set authentication from JWT claims: {}", e.getMessage());
            // Do not propagate — let SecurityContext remain empty
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
