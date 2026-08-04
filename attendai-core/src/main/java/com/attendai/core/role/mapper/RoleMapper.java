package com.attendai.core.role.mapper;

import com.attendai.core.role.dto.RoleResponse;
import com.attendai.core.role.dto.RoleSummaryResponse;
import com.attendai.core.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    @Mapping(target = "isSystem", source = "system")
    RoleResponse toResponse(Role role);

    @Mapping(target = "isSystem", source = "system")
    RoleSummaryResponse toSummaryResponse(Role role);
}
