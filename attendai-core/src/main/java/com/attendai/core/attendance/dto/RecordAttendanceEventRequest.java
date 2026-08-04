package com.attendai.core.attendance.dto;

import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Request body for recording an attendance event submitted by a station. */
@Getter
@Setter
public class RecordAttendanceEventRequest {

    @NotNull(message = "Person ID is required")
    private Long personId;

    @NotNull(message = "Event time is required")
    private LocalDateTime eventTime;

    @NotNull(message = "Direction is required")
    private EventDirection direction;

    @NotNull(message = "Source is required")
    private EventSource source;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
