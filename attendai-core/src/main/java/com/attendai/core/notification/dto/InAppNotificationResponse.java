package com.attendai.core.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InAppNotificationResponse {
    private final Long          id;
    private final String        typeCode;
    private final String        title;
    private final String        body;
    private final boolean       isRead;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;
}
