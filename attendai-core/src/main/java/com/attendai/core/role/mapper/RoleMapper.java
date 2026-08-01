package com.attendai.core.role.mapper;

import com.attendai.core.role.dto.RoleResponse;
import com.attendai.core.role.dto.RoleSummaryResponse;
import com.attendai.core.role.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toResponse(Role role);
    RoleSummaryResponse toSummaryResponse(Role role);
}
