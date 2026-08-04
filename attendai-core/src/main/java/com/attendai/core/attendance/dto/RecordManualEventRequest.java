package com.attendai.core.attendance.dto;

import com.attendai.core.attendance.entity.EventDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Request body for manually recording an attendance event by an operator. */
@Getter
@Setter
public class RecordManualEventRequest {

    @NotNull(message = "Person ID is required")
    private Long personId;

    @NotNull(message = "Event time is required")
    private LocalDateTime eventTime;

    @NotNull(message = "Direction is required")
    private EventDirection direction;

    /** Optional station; null is permitted for manual events. */
    private Long stationId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
