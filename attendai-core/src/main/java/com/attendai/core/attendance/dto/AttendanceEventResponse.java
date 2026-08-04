package com.attendai.core.attendance.dto;

import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Full attendance event response. */
@Getter
@Builder
public class AttendanceEventResponse {
    private final Long                   id;
    private final Long                   personId;
    private final Long                   stationId;
    private final LocalDateTime          eventTime;
    private final EventDirection         direction;
    private final EventSource            source;
    private final AttendanceEventStatus  status;
    private final String                 rejectionReason;
    private final String                 notes;
    private final Long                   originalEventId;
    private final LocalDateTime          processedAt;
    private final String                 processedBy;
    private final LocalDateTime          createdAt;
}
