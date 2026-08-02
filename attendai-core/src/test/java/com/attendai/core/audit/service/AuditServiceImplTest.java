package com.attendai.core.audit.service;

import com.attendai.core.audit.config.AuditProperties;
import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.entity.AuditLog;
import com.attendai.core.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock AuditLogRepository auditLogRepository;

    private AuditProperties   auditProperties;
    private AuditServiceImpl  auditService;

    @BeforeEach
    void setUp() {
        auditProperties = new AuditProperties();
        auditService = new AuditServiceImpl(auditLogRepository, auditProperties);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Core contract: log() must never throw
    // -------------------------------------------------------------------------

    @Test
    void log_shouldNeverThrow_whenRepositorySaveSucceeds() {
        when(auditLogRepository.save(any())).thenReturn(new AuditLog());

        assertThatCode(() -> auditService.log(AuditEventRequest.builder()
                .actionCode("TEST_ACTION")
                .module("core-test")
                .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void log_shouldNeverThrow_whenRepositoryThrowsException() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB is down"));

        assertThatCode(() -> auditService.log(AuditEventRequest.builder()
                .actionCode("TEST_ACTION")
                .module("core-test")
                .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void log_shouldNeverThrow_whenActionCodeIsNull() {
        // Even a malformed request must not crash the caller
        assertThatCode(() -> auditService.log(AuditEventRequest.builder()
                .actionCode(null)
                .module("core-test")
                .build()))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Record building
    // -------------------------------------------------------------------------

    @Test
    void log_shouldPersistAllProvidedFields() {
        LocalDateTime now = LocalDateTime.now();
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditEventRequest request = AuditEventRequest.builder()
                .actorUserId(42L)
                .actionCode("USER_CREATED")
                .resourceType("User")
                .resourceId("42")
                .module("core-user")
                .ipAddress("10.0.0.1")
                .details("{\"field\":\"value\"}")
                .occurredAt(now)
                .build();

        auditService.log(request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getActorUserId()).isEqualTo(42L);
        assertThat(saved.getActionCode()).isEqualTo("USER_CREATED");
        assertThat(saved.getResourceType()).isEqualTo("User");
        assertThat(saved.getResourceId()).isEqualTo("42");
        assertThat(saved.getModule()).isEqualTo("core-user");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(saved.getDetails()).isEqualTo("{\"field\":\"value\"}");
        assertThat(saved.getOccurredAt()).isEqualTo(now);
    }

    @Test
    void log_shouldDefaultOccurredAt_whenNotProvided() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_CREATED")
                .module("core-user")
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getOccurredAt()).isAfter(before);
    }

    // -------------------------------------------------------------------------
    // Actor resolution from SecurityContext
    // -------------------------------------------------------------------------

    @Test
    void log_shouldResolveActorFromSecurityContext_whenNotExplicitlyProvided() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(7L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(AuditEventRequest.builder()
                .actionCode("SOME_ACTION")
                .module("core-test")
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isEqualTo(7L);
    }

    @Test
    void log_shouldUseExplicitActorId_overridingSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(99L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(AuditEventRequest.builder()
                .actorUserId(5L)   // explicitly provided
                .actionCode("SOME_ACTION")
                .module("core-test")
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // The explicitly provided actor ID takes precedence over the security context
        assertThat(captor.getValue().getActorUserId()).isEqualTo(5L);
    }

    @Test
    void log_shouldSetActorNull_whenNoAuthenticationPresent() {
        SecurityContextHolder.clearContext();
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHEDULED_JOB")
                .module("core-attendance")
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isNull();
    }

    // -------------------------------------------------------------------------
    // Details truncation
    // -------------------------------------------------------------------------

    @Test
    void log_shouldTruncateDetails_whenExceedsMaxLength() {
        auditProperties.setMaxDetailsLength(10);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String longDetails = "A".repeat(100);
        auditService.log(AuditEventRequest.builder()
                .actionCode("ACTION")
                .module("core-test")
                .details(longDetails)
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).hasSize(10);
    }

    @Test
    void log_shouldNotTruncateDetails_whenWithinMaxLength() {
        auditProperties.setMaxDetailsLength(100);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String shortDetails = "short";
        auditService.log(AuditEventRequest.builder()
                .actionCode("ACTION")
                .module("core-test")
                .details(shortDetails)
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isEqualTo("short");
    }

    @Test
    void log_shouldHandleNullDetails() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACTION")
                .module("core-test")
                .details(null)
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isNull();
    }
}
