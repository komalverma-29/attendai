package com.attendai.core.file.storage;

import java.io.InputStream;

/**
 * Abstraction over the underlying binary file storage mechanism.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link LocalFileStorageBackend} — stores files on the local filesystem</li>
 *   <li>S3FileStorageBackend (future) — stores files in an S3-compatible bucket</li>
 * </ul>
 *
 * <p>The active implementation is selected via the {@code attendai.file.storage-backend}
 * configuration property. Swapping implementations requires only a config change.
 */
public interface FileStorageBackend {

    /**
     * Stores the input stream under the given storage key.
     *
     * @param inputStream  binary content to store
     * @param storageKey   unique key identifying where to store the file
     * @param contentType  MIME type of the content
     * @param sizeBytes    expected size (may be used for validation)
     * @return the confirmed storage key
     */
    String store(InputStream inputStream, String storageKey, String contentType, long sizeBytes);

    /**
     * Returns an InputStream for the file identified by the given storage key.
     *
     * @param storageKey the unique storage key
     * @return an open InputStream; the caller is responsible for closing it
     */
    InputStream retrieve(String storageKey);

    /**
     * Permanently removes the file from the storage backend.
     *
     * @param storageKey the unique storage key
     */
    void delete(String storageKey);

    /**
     * Generates a time-limited URL for direct access to the file.
     *
     * <p>For local storage this returns an API-proxied URL.
     * For S3 this returns a pre-signed object URL.
     *
     * @param storageKey the unique storage key
     * @param ttlSeconds how long the URL should remain valid
     * @return a URL string
     */
    String generatePresignedUrl(String storageKey, long ttlSeconds);
}
