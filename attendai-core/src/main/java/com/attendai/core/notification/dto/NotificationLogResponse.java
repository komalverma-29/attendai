package com.attendai.core.notification.dto;

import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.NotificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationLogResponse {
    private final Long               id;
    private final Long               recipientUserId;
    private final String             typeCode;
    private final Channel            channel;
    private final NotificationStatus status;
    private final String             subject;
    private final int                attemptCount;
    private final String             errorMessage;
    private final LocalDateTime      scheduledAt;
    private final LocalDateTime      sentAt;
    private final LocalDateTime      createdAt;
}
