package com.attendai.core.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Utility class for accessing the Spring Security context without coupling
 * service classes directly to the HTTP request or authentication infrastructure.
 *
 * All methods are static and safe to call from any layer.
 * No exception is thrown when no authentication is present — callers receive
 * {@link Optional#empty()} or an empty collection instead.
 *
 * Never log or expose the raw {@link Authentication} object — it may contain
 * sensitive credential data.
 */
public final class SecurityContextUtils {

    private SecurityContextUtils() {
        // Utility class — no instantiation
    }

    /**
     * Returns the ID of the currently authenticated user, or {@link Optional#empty()}
     * when no authentication is present (unauthenticated requests, scheduled jobs).
     *
     * <p>The principal stored in the {@link Authentication} object is expected to be
     * a {@link Long} representing the user's surrogate PK. This is set by
     * {@code JwtAuthenticationFilter} when a valid JWT is validated.
     *
     * @return {@link Optional} containing the user ID, or empty if unauthenticated
     */
    public static Optional<Long> getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long userId) {
                return Optional.of(userId);
            }
            // Handle String-encoded user ID (e.g., from @WithMockUser in tests)
            if (principal instanceof String principalStr && !"anonymousUser".equals(principalStr)) {
                try {
                    return Optional.of(Long.parseLong(principalStr));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
            // Handle UserDetails (e.g., from @WithMockUser) — parse username as user ID
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                try {
                    return Optional.of(Long.parseLong(ud.getUsername()));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the authorities (permission codes) of the currently authenticated user.
     * Returns an empty collection when no authentication is present.
     *
     * @return collection of {@link GrantedAuthority} objects, never null
     */
    public static Collection<? extends GrantedAuthority> getCurrentUserAuthorities() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Collections.emptyList();
            }
            return authentication.getAuthorities();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns {@code true} if there is an authenticated (non-anonymous) user
     * in the current security context.
     *
     * @return {@code true} if authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId().isPresent();
    }

    /**
     * Returns {@code true} if the current user holds the given authority.
     *
     * @param authority the authority string to check (e.g., "SCHOOL_STUDENT_READ")
     * @return {@code true} if the current user has the authority
     */
    public static boolean hasAuthority(String authority) {
        return getCurrentUserAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
