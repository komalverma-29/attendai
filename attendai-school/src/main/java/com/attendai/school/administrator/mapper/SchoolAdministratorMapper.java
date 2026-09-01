package com.attendai.school.administrator.mapper;

import com.attendai.school.administrator.dto.AdministratorResponse;
import com.attendai.school.administrator.dto.AdministratorSummaryResponse;
import com.attendai.school.administrator.entity.SchoolAdministrator;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolAdministratorMapper {
    AdministratorResponse       toResponse(SchoolAdministrator admin);
    AdministratorSummaryResponse toSummaryResponse(SchoolAdministrator admin);
}
