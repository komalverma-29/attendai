package com.attendai.core.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AttendAIAuditorAware.
 * Verifies that the current auditor is resolved from the SecurityContext.
 */
class AttendAIAuditorAwareTest {

    private final AttendAIAuditorAware auditorAware = new AttendAIAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_shouldReturnUserId_whenAuthenticated() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(7L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent().contains(7L);
    }

    @Test
    void getCurrentAuditor_shouldReturnEmpty_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isEmpty();
    }

    @Test
    void getCurrentAuditor_shouldReturnEmpty_whenPrincipalIsAnonymous() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isEmpty();
    }
}
