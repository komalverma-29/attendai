package com.attendai.core.notification.dto;

import com.attendai.core.notification.entity.Channel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPreferenceResponse {
    private final Long    id;
    private final String  typeCode;
    private final Channel channel;
    private final boolean isEnabled;
}
