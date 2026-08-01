package com.attendai.core.user.dto;

import com.attendai.core.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

/** Lightweight user summary used in paginated list responses. */
@Getter
@Builder
public class UserSummaryResponse {

    private final Long id;
    private final Long personId;
    private final String email;
    private final String username;
    private final UserStatus status;
}
