package com.attendai.core.config.mapper;

import com.attendai.core.config.dto.SystemConfigResponse;
import com.attendai.core.config.dto.SystemConfigSummaryResponse;
import com.attendai.core.config.entity.SystemConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SystemConfigMapper {
    SystemConfigResponse       toResponse(SystemConfig config);
    SystemConfigSummaryResponse toSummaryResponse(SystemConfig config);
}
