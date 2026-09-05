package com.attendai.school.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PendingActionsResponse {
    private final long correctionRequests;
    private final long leaveApplications;
    private final long consecutiveAbsenceAlerts;
}
