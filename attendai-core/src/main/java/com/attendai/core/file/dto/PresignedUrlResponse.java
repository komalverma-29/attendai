package com.attendai.core.file.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PresignedUrlResponse {
    private final String        url;
    private final LocalDateTime expiresAt;
}
