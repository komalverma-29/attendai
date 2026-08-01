package com.attendai.core.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response returned on successful login or token refresh.
 */
@Getter
@Builder
public class AuthTokenResponse {

    private final String accessToken;
    private final String refreshToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    /** Access token TTL in seconds. */
    private final long expiresIn;
}
