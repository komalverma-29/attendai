package com.attendai.core.audit.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.entity.AuditLog;
import com.attendai.core.audit.repository.AuditLogRepository;
import com.attendai.core.common.security.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Default implementation of {@link AuditService}.
 *
 * <p>Resolves actor user ID and IP address from the security/request context
 * when not explicitly provided. Catches all persistence exceptions to ensure
 * the caller's operation is never disrupted by audit log failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(AuditEventRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actorUserId(resolveActorUserId(request))
                    .actionCode(request.getActionCode())
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId())
                    .module(request.getModule())
                    .ipAddress(resolveIpAddress(request))
                    .details(request.getDetails())
                    .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            // Never propagate audit failures to the caller
            log.error("Failed to persist audit event [{}]: {}", request.getActionCode(), e.getMessage(), e);
        }
    }

    private Long resolveActorUserId(AuditEventRequest request) {
        if (request.getActorUserId() != null) {
            return request.getActorUserId();
        }
        return SecurityContextUtils.getCurrentUserId().orElse(null);
    }

    private String resolveIpAddress(AuditEventRequest request) {
        if (request.getIpAddress() != null) {
            return request.getIpAddress();
        }
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest httpRequest = attributes.getRequest();
                String forwarded = httpRequest.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    // Take the first IP in the chain (client IP)
                    return forwarded.split(",")[0].trim();
                }
                return httpRequest.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // No request context (e.g., scheduled job) — IP is null
        }
        return null;
    }
}
