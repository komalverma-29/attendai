package com.attendai.core.file.storage;

import com.attendai.core.common.exception.ExternalServiceException;
import com.attendai.core.file.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Local filesystem implementation of {@link FileStorageBackend}.
 *
 * <p>Active when {@code attendai.file.storage-backend=local} (the default).
 *
 * <p>Files are stored under the configured base path with the storage key as
 * the relative subdirectory and filename. Parent directories are created
 * automatically on first write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "attendai.file.storage-backend",
        havingValue = "local", matchIfMissing = true)
public class LocalFileStorageBackend implements FileStorageBackend {

    private final FileProperties fileProperties;

    @Override
    public String store(InputStream inputStream, String storageKey,
                        String contentType, long sizeBytes) {
        Path target = resolvePath(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(target,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                inputStream.transferTo(out);
            }
            log.debug("File stored locally | key={}", storageKey);
            return storageKey;
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to store file locally: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        Path target = resolvePath(storageKey);
        try {
            return new FileInputStream(target.toFile());
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to retrieve file: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolvePath(storageKey);
        try {
            Files.deleteIfExists(target);
            log.debug("File deleted locally | key={}", storageKey);
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedUrl(String storageKey, long ttlSeconds) {
        // For local storage, return an API-proxied download URL.
        // The ID-based URL is resolved at the API layer — storage key is not exposed.
        return "/api/v1/core/files/download?key=" + storageKey + "&ttl=" + ttlSeconds;
    }

    private Path resolvePath(String storageKey) {
        return Paths.get(fileProperties.getLocalBasePath()).resolve(storageKey).normalize();
    }
}
