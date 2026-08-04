package com.attendai.core.permission.mapper;

import com.attendai.core.permission.dto.PermissionResponse;
import com.attendai.core.permission.dto.PermissionSummaryResponse;
import com.attendai.core.permission.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {

    @Mapping(target = "isSystem", source = "system")
    PermissionResponse toResponse(Permission permission);

    @Mapping(target = "isSystem", source = "system")
    PermissionSummaryResponse toSummaryResponse(Permission permission);
}
