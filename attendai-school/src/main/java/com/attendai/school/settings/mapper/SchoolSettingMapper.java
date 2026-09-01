package com.attendai.school.settings.mapper;

import com.attendai.school.settings.dto.SchoolSettingResponse;
import com.attendai.school.settings.entity.SchoolSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link SchoolSetting}.
 *
 * <p>The {@code defaultValue} field is not stored in the entity — it is computed
 * by the service and passed separately when building responses.
 * The {@code key} and {@code value} fields map to {@code settingKey} and
 * {@code settingValue} on the entity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolSettingMapper {

    @Mapping(target = "key",          source = "setting.settingKey")
    @Mapping(target = "value",        source = "setting.settingValue")
    @Mapping(target = "description",  source = "setting.description")
    @Mapping(target = "createdAt",    source = "setting.createdAt")
    @Mapping(target = "updatedAt",    source = "setting.updatedAt")
    @Mapping(target = "defaultValue", source = "defaultValue")
    SchoolSettingResponse toResponse(SchoolSetting setting, String defaultValue);
}
