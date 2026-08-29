package com.attendai.core.notification.mapper;

import com.attendai.core.notification.dto.InAppNotificationResponse;
import com.attendai.core.notification.dto.NotificationLogResponse;
import com.attendai.core.notification.dto.NotificationPreferenceResponse;
import com.attendai.core.notification.dto.NotificationTemplateResponse;
import com.attendai.core.notification.entity.InAppNotification;
import com.attendai.core.notification.entity.NotificationLog;
import com.attendai.core.notification.entity.NotificationPreference;
import com.attendai.core.notification.entity.NotificationTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "isRead", source = "read")
    InAppNotificationResponse toInAppResponse(InAppNotification notification);

    @Mapping(target = "isActive", source = "active")
    NotificationTemplateResponse toTemplateResponse(NotificationTemplate template);

    @Mapping(target = "isEnabled", source = "enabled")
    NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference);

    NotificationLogResponse toLogResponse(NotificationLog log);
}
