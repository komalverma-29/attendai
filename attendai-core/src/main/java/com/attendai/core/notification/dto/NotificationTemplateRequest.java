package com.attendai.core.notification.dto;

import com.attendai.core.notification.entity.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationTemplateRequest {

    @NotBlank(message = "typeCode is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "typeCode must be uppercase letters, digits, and underscores")
    private String typeCode;

    @NotNull(message = "channel is required")
    private Channel channel;

    @NotBlank(message = "locale is required")
    @Size(max = 10)
    private String locale = "en";

    /** Required for EMAIL channel. */
    @Size(max = 255)
    private String subject;

    @NotBlank(message = "bodyTemplate is required")
    private String bodyTemplate;
}
