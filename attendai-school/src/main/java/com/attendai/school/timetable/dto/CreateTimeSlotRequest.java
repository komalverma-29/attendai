package com.attendai.school.timetable.dto;

import com.attendai.school.timetable.entity.TimeSlotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CreateTimeSlotRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Slot order is required")
    private Integer slotOrder;

    /** Default PERIOD if not supplied. */
    private TimeSlotType slotType = TimeSlotType.PERIOD;
}
