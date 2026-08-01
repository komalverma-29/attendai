package com.attendai.core.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityContextUtils.
 * Each test manually sets up and tears down the SecurityContext.
 */
class SecurityContextUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_shouldReturnEmpty_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        Optional<Long> userId = SecurityContextUtils.getCurrentUserId();

        assertThat(userId).isEmpty();
    }

    @Test
    void getCurrentUserId_shouldReturnUserId_whenAuthenticatedWithLongPrincipal() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(42L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> userId = SecurityContextUtils.getCurrentUserId();

        assertThat(userId).isPresent().contains(42L);
    }

    @Test
    void getCurrentUserId_shouldReturnUserId_whenAuthenticatedWithStringPrincipal() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("99", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> userId = SecurityContextUtils.getCurrentUserId();

        assertThat(userId).isPresent().contains(99L);
    }

    @Test
    void getCurrentUserId_shouldReturnEmpty_whenPrincipalIsAnonymousUser() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> userId = SecurityContextUtils.getCurrentUserId();

        assertThat(userId).isEmpty();
    }

    @Test
    void getCurrentUserAuthorities_shouldReturnAuthorities_whenAuthenticated() {
        var authorities = List.of(
                new SimpleGrantedAuthority("SCHOOL_STUDENT_READ"),
                new SimpleGrantedAuthority("SCHOOL_ATTENDANCE_READ")
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        var result = SecurityContextUtils.getCurrentUserAuthorities();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("authority")
                .containsExactlyInAnyOrder("SCHOOL_STUDENT_READ", "SCHOOL_ATTENDANCE_READ");
    }

    @Test
    void getCurrentUserAuthorities_shouldReturnEmpty_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        var result = SecurityContextUtils.getCurrentUserAuthorities();

        assertThat(result).isEmpty();
    }

    @Test
    void isAuthenticated_shouldReturnTrue_whenValidUserAuthenticated() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(5L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextUtils.isAuthenticated()).isTrue();
    }

    @Test
    void isAuthenticated_shouldReturnFalse_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThat(SecurityContextUtils.isAuthenticated()).isFalse();
    }

    @Test
    void hasAuthority_shouldReturnTrue_whenAuthorityPresent() {
        var authorities = List.of(new SimpleGrantedAuthority("SCHOOL_STUDENT_READ"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityContextUtils.hasAuthority("SCHOOL_STUDENT_READ")).isTrue();
        assertThat(SecurityContextUtils.hasAuthority("SCHOOL_STUDENT_DELETE")).isFalse();
    }
}
