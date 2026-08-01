package com.attendai.core.audit.service;

import com.attendai.core.audit.dto.AuditEventRequest;

/**
 * Platform-wide audit logging service.
 *
 * <p>Every module — Core and business — calls {@link #log(AuditEventRequest)} to
 * record significant events. The implementation guarantees:
 * <ul>
 *   <li>This method NEVER throws to the caller. Failures are logged internally.</li>
 *   <li>Records are immutable once written (append-only table).</li>
 *   <li>The actor user ID and IP address are resolved from the security context
 *       when not explicitly provided in the request.</li>
 * </ul>
 *
 * <p>Use {@link AuditEventRequest#builder()} to construct the request:
 * <pre>{@code
 * auditService.log(AuditEventRequest.builder()
 *     .actionCode("AUTH_LOGIN_SUCCESS")
 *     .module("core-auth")
 *     .resourceType("User")
 *     .resourceId(String.valueOf(userId))
 *     .build());
 * }</pre>
 */
public interface AuditService {

    /**
     * Writes an audit event record.
     *
     * <p>This method is fire-and-forget. It never throws an exception to the caller.
     * If persistence fails, the error is logged at ERROR level and the calling
     * operation continues normally.
     *
     * @param request the audit event details
     */
    void log(AuditEventRequest request);
}
