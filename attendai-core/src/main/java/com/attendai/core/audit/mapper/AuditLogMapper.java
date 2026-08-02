package com.attendai.core.audit.mapper;

import com.attendai.core.audit.dto.AuditLogResponse;
import com.attendai.core.audit.entity.AuditLog;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper from {@link AuditLog} entity to {@link AuditLogResponse} DTO.
 *
 * All fields map by name — no custom mappings required.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
