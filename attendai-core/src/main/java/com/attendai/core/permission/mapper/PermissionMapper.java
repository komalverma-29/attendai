package com.attendai.core.permission.mapper;

import com.attendai.core.permission.dto.PermissionResponse;
import com.attendai.core.permission.dto.PermissionSummaryResponse;
import com.attendai.core.permission.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toResponse(Permission permission);
    PermissionSummaryResponse toSummaryResponse(Permission permission);
}
