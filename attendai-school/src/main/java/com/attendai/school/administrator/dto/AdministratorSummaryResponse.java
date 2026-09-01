package com.attendai.school.administrator.dto;

import com.attendai.school.administrator.entity.AdministratorStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdministratorSummaryResponse {
    private final Long                id;
    private final Long                personId;
    private final Long                userId;
    private final String              designation;
    private final AdministratorStatus status;
}
