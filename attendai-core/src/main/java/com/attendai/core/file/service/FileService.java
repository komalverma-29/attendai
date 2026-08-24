package com.attendai.core.file.service;

import com.attendai.core.file.dto.FileMetadataResponse;
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.dto.PresignedUrlResponse;
import com.attendai.core.file.entity.FileVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Core file management service.
 *
 * Handles upload, metadata retrieval, download, deletion, and access control.
 * Exposes an internal API for other Core modules (e.g., core-face) to store
 * and retrieve file content without going through HTTP.
 */
public interface FileService {

    // HTTP-facing operations

    FileUploadResponse upload(MultipartFile file, FileVisibility visibility,
                              String module, Long uploadedByUserId);

    FileMetadataResponse getMetadata(Long fileId, Long requestingUserId, boolean isAdmin);

    InputStream download(Long fileId, Long requestingUserId, boolean isAdmin);

    void deleteFile(Long fileId, Long requestingUserId, boolean isAdmin);

    PresignedUrlResponse generatePresignedUrl(Long fileId, Long ttlSeconds,
                                              Long requestingUserId, boolean isAdmin);

    Page<FileMetadataResponse> listOwnFiles(Long userId, String module, Pageable pageable);

    // Internal API for other Core modules (called as Spring beans)

    boolean existsById(Long fileId);

    InputStream retrieveStream(Long fileId);

    void deleteById(Long fileId);
}
