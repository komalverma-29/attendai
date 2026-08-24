package com.attendai.core.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * File management configuration properties.
 * Bound from {@code application.yml} under the prefix {@code attendai.file}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.file")
public class FileProperties {

    /**
     * Storage backend to use: "local" or "s3".
     * Default: "local".
     */
    private String storageBackend = "local";

    /** Maximum allowed file upload size in bytes. Default: 10 MB. */
    private long maxSizeBytes = 10_485_760L;

    /**
     * Comma-separated list of allowed MIME types.
     * Default: image/jpeg, image/png, image/webp, application/pdf.
     */
    private List<String> allowedTypes = List.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    /** Maximum TTL for pre-signed URLs in seconds. Default: 3600 (1 hour). */
    private long presignedUrlMaxTtl = 3_600L;

    /** Base directory for local file storage. */
    private String localBasePath = "/var/attendai/files";
}
