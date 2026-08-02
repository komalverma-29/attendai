package com.attendai.core.audit.controller;

import com.attendai.core.audit.dto.AuditLogResponse;
import com.attendai.core.audit.entity.AuditLog;
import com.attendai.core.audit.mapper.AuditLogMapper;
import com.attendai.core.audit.repository.AuditLogRepository;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Read-only REST controller for the audit log.
 *
 * Base path: /api/v1/core/audit
 *
 * There is NO write endpoint. All audit writes go through the internal
 * {@link com.attendai.core.audit.service.AuditService} Spring bean.
 *
 * Access is restricted to {@code CORE_AUDIT_READ} — system administrators only.
 *
 * Note: all @RequestParam and @PathVariable annotations carry explicit {@code name}
 * values to ensure Spring MVC can resolve parameter names without relying on
 * bytecode reflection (required for full compatibility across all JVM versions).
 */
@RestController
@RequestMapping("/api/v1/core/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper     auditLogMapper;

    /**
     * GET /api/v1/core/audit/logs
     *
     * Query the audit log with optional filters. All parameters are optional.
     * Results are ordered by {@code occurredAt} descending (most recent first).
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('CORE_AUDIT_READ')")
    public ResponseEntity<PageResponse<AuditLogResponse>> queryLogs(
            @RequestParam(name = "actorUserId",  required = false) Long actorUserId,
            @RequestParam(name = "actionCode",   required = false) String actionCode,
            @RequestParam(name = "resourceType", required = false) String resourceType,
            @RequestParam(name = "resourceId",   required = false) String resourceId,
            @RequestParam(name = "module",       required = false) String module,
            @RequestParam(name = "fromDate",     required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "toDate",       required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Valid PageRequestParams pageParams) {

        Page<AuditLog> page = auditLogRepository.findByFilters(
                actorUserId, actionCode, resourceType, module, fromDate, toDate,
                pageParams.toPageable());

        return ResponseEntity.ok(PageResponse.of(page.map(auditLogMapper::toResponse)));
    }

    /**
     * GET /api/v1/core/audit/logs/{id}
     *
     * Retrieve a single audit log entry by its surrogate ID.
     */
    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAuthority('CORE_AUDIT_READ')")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getLog(
            @PathVariable("id") Long id) {

        AuditLog entry = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Audit log entry with id " + id + " was not found"));

        return ResponseEntity.ok(ApiResponse.success(auditLogMapper.toResponse(entry)));
    }
}
