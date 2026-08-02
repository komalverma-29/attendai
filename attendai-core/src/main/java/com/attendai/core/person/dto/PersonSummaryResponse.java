package com.attendai.core.person.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight person summary used in paginated list responses.
 */
@Getter
@Builder
public class PersonSummaryResponse {
    private final Long id;
    private final String fullName;
    private final String email;
    private final String phone;
}
