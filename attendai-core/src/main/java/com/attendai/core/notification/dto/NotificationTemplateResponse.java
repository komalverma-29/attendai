package com.attendai.core.notification.dto;

import com.attendai.core.notification.entity.Channel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationTemplateResponse {
    private final Long          id;
    private final String        typeCode;
    private final Channel       channel;
    private final String        locale;
    private final String        subject;
    private final String        bodyTemplate;
    private final boolean       isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
