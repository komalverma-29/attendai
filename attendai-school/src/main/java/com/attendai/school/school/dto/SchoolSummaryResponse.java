package com.attendai.school.school.dto;

import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import lombok.Builder;
import lombok.Getter;

/** Lightweight school summary for paginated list responses. */
@Getter
@Builder
public class SchoolSummaryResponse {
    private final Long         id;
    private final String       name;
    private final String       code;
    private final SchoolType   type;
    private final SchoolStatus status;
    private final String       city;
    private final String       country;
}
