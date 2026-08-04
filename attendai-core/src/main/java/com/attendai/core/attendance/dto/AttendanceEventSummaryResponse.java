package com.attendai.core.attendance.dto;

import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Lightweight attendance event summary used in paginated list responses. */
@Getter
@Builder
public class AttendanceEventSummaryResponse {
    private final Long                   id;
    private final Long                   personId;
    private final Long                   stationId;
    private final LocalDateTime          eventTime;
    private final EventDirection         direction;
    private final EventSource            source;
    private final AttendanceEventStatus  status;
}
