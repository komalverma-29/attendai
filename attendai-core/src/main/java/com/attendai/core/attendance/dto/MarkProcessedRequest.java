package com.attendai.core.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body for marking an event as processed by a business module. */
@Getter
@Setter
public class MarkProcessedRequest {

    @NotBlank(message = "processedBy is required")
    @Size(max = 50, message = "processedBy must not exceed 50 characters")
    private String processedBy;
}
