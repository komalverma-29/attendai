package com.attendai.core.notification.dto;

import com.attendai.core.notification.entity.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferenceRequest {

    @NotBlank(message = "typeCode is required")
    @Size(max = 100)
    private String typeCode;

    @NotNull(message = "channel is required")
    private Channel channel;

    @NotNull(message = "isEnabled is required")
    private Boolean isEnabled;
}
