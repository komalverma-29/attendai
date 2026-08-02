package com.attendai.core.audit.service;

import com.attendai.core.audit.config.AuditProperties;
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
 * <p>Key contract: {@link #log(AuditEventRequest)} NEVER throws to the caller.
 * Any persistence failure is caught, logged at ERROR level, and swallowed so
 * the calling operation can continue normally.
 *
 * <p>Actor user ID and IP address are resolved from the security / request
 * context when not explicitly provided in the request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditProperties    auditProperties;

    @Override
    public void log(AuditEventRequest request) {
        try {
            String details = truncateDetails(request.getDetails());

            AuditLog auditLog = AuditLog.builder()
                    .actorUserId(resolveActorUserId(request))
                    .actionCode(request.getActionCode())
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId())
                    .module(request.getModule())
                    .ipAddress(resolveIpAddress(request))
                    .details(details)
                    .occurredAt(request.getOccurredAt() != null
                            ? request.getOccurredAt()
                            : LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            // Never propagate audit failures to the caller
            log.error("Failed to persist audit event [{}]: {}",
                    request.getActionCode(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
            if (attributes == null) {
                return null;
            }
            HttpServletRequest httpRequest = attributes.getRequest();
            if (auditProperties.isTrustProxy()) {
                String forwarded = httpRequest.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    // First entry in the chain is the original client IP
                    return forwarded.split(",")[0].trim();
                }
            }
            return httpRequest.getRemoteAddr();
        } catch (Exception ignored) {
            // No request context (e.g., scheduled job) — IP is null
            return null;
        }
    }

    /**
     * Truncates the details JSON string to the configured maximum length.
     * Oversized details are silently truncated — callers should keep details concise.
     */
    private String truncateDetails(String details) {
        if (details == null) {
            return null;
        }
        int maxLen = auditProperties.getMaxDetailsLength();
        if (details.length() <= maxLen) {
            return details;
        }
        log.warn("Audit event details truncated from {} to {} characters", details.length(), maxLen);
        return details.substring(0, maxLen);
    }
}
