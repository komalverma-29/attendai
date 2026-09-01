package com.attendai.school.administrator.dto;

import com.attendai.school.administrator.entity.AdministratorStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdministratorResponse {
    private final Long                id;
    private final Long                schoolId;
    private final Long                personId;
    private final Long                userId;
    private final String              designation;
    private final AdministratorStatus status;
    private final String              notes;
    private final LocalDateTime       createdAt;
    private final LocalDateTime       updatedAt;
}
