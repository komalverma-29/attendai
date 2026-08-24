package com.attendai.core.file.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ForbiddenException;
import com.attendai.core.common.util.StringUtils;
import com.attendai.core.file.config.FileProperties;
import com.attendai.core.file.dto.FileMetadataResponse;
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.dto.PresignedUrlResponse;
import com.attendai.core.file.entity.FileRecord;
import com.attendai.core.file.entity.FileVisibility;
import com.attendai.core.file.exception.FileNotFoundException;
import com.attendai.core.file.exception.FileTypeNotAllowedException;
import com.attendai.core.file.mapper.FileMapper;
import com.attendai.core.file.repository.FileRecordRepository;
import com.attendai.core.file.storage.FileStorageBackend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordRepository fileRecordRepository;
    private final FileStorageBackend   fileStorageBackend;
    private final FileProperties       fileProperties;
    private final FileMapper           fileMapper;
    private final AuditService         auditService;

    private static final DateTimeFormatter DATE_PATH_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FileUploadResponse upload(MultipartFile file, FileVisibility visibility,
                                     String module, Long uploadedByUserId) {
        // Validate content type against allowlist
        String contentType = file.getContentType();
        if (contentType == null || !fileProperties.getAllowedTypes().contains(contentType)) {
            throw new FileTypeNotAllowedException(contentType);
        }

        // Validate file size
        if (file.getSize() > fileProperties.getMaxSizeBytes()) {
            throw new com.attendai.core.common.exception.ValidationException(
                    "File size exceeds the maximum allowed size of "
                    + fileProperties.getMaxSizeBytes() + " bytes");
        }

        // Sanitise filename and generate a unique storage key
        String sanitisedName = StringUtils.sanitiseFilename(file.getOriginalFilename());
        if (sanitisedName == null || sanitisedName.isBlank()) {
            sanitisedName = "upload";
        }
        String ext        = extractExtension(sanitisedName);
        String datePrefix = LocalDateTime.now().format(DATE_PATH_FORMAT);
        String modulePart = (module != null && !module.isBlank()) ? module : "core";
        String storageKey = modulePart + "/" + datePrefix + "/" + UUID.randomUUID() + ext;

        // Stream to storage backend
        try (InputStream inputStream = file.getInputStream()) {
            fileStorageBackend.store(inputStream, storageKey, contentType, file.getSize());
        } catch (Exception e) {
            throw new com.attendai.core.common.exception.ExternalServiceException(
                    "Failed to store file: " + e.getMessage(), e);
        }

        FileRecord record = FileRecord.builder()
                .originalName(sanitisedName)
                .storageKey(storageKey)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .visibility(visibility != null ? visibility : FileVisibility.PRIVATE)
                .uploadedByUserId(uploadedByUserId)
                .module(module)
                .build();

        FileRecord saved = fileRecordRepository.save(record);

        log.info("File uploaded | fileId={} module={} size={}", saved.getId(), module, file.getSize());

        auditService.log(AuditEventRequest.builder()
                .actionCode("FILE_UPLOADED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("File")
                .resourceId(String.valueOf(saved.getId()))
                .actorUserId(uploadedByUserId)
                .details("{\"module\":\"" + module + "\",\"sizeBytes\":" + file.getSize() + "}")
                .build());

        return fileMapper.toUploadResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public FileMetadataResponse getMetadata(Long fileId, Long requestingUserId, boolean isAdmin) {
        FileRecord record = requireFile(fileId);
        checkReadAccess(record, requestingUserId, isAdmin);
        return fileMapper.toMetadataResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream download(Long fileId, Long requestingUserId, boolean isAdmin) {
        FileRecord record = requireFile(fileId);
        checkReadAccess(record, requestingUserId, isAdmin);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FILE_DOWNLOADED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("File")
                .resourceId(String.valueOf(fileId))
                .actorUserId(requestingUserId)
                .build());

        return fileStorageBackend.retrieve(record.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUrlResponse generatePresignedUrl(Long fileId, Long ttlSeconds,
                                                     Long requestingUserId, boolean isAdmin) {
        FileRecord record = requireFile(fileId);
        checkReadAccess(record, requestingUserId, isAdmin);

        long ttl = Math.min(ttlSeconds != null ? ttlSeconds : 300L,
                fileProperties.getPresignedUrlMaxTtl());

        String url = fileStorageBackend.generatePresignedUrl(record.getStorageKey(), ttl);

        return PresignedUrlResponse.builder()
                .url(url)
                .expiresAt(LocalDateTime.now().plusSeconds(ttl))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> listOwnFiles(Long userId, String module, Pageable pageable) {
        return fileRecordRepository
                .findByUploadedByUserIdAndModule(userId, module, pageable)
                .map(fileMapper::toMetadataResponse);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long requestingUserId, boolean isAdmin) {
        FileRecord record = requireFile(fileId);

        boolean isOwner = record.getUploadedByUserId().equals(requestingUserId);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You do not have permission to delete this file");
        }

        record.softDelete();
        fileRecordRepository.save(record);

        log.info("File soft-deleted | fileId={}", fileId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("FILE_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("File")
                .resourceId(String.valueOf(fileId))
                .actorUserId(requestingUserId)
                .build());
    }

    // -------------------------------------------------------------------------
    // Internal API
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long fileId) {
        return fileRecordRepository.existsById(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream retrieveStream(Long fileId) {
        FileRecord record = requireFile(fileId);
        return fileStorageBackend.retrieve(record.getStorageKey());
    }

    @Override
    @Transactional
    public void deleteById(Long fileId) {
        fileRecordRepository.findById(fileId).ifPresent(record -> {
            record.softDelete();
            fileRecordRepository.save(record);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FileRecord requireFile(Long id) {
        return fileRecordRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException(id));
    }

    /**
     * Checks whether the requesting user can read the file.
     * PUBLIC files are accessible to all. PRIVATE files require ownership or admin.
     */
    private void checkReadAccess(FileRecord record, Long requestingUserId, boolean isAdmin) {
        if (record.getVisibility() == FileVisibility.PUBLIC) {
            return; // no auth required
        }
        // PRIVATE: owner or admin only
        boolean isOwner = requestingUserId != null
                && record.getUploadedByUserId().equals(requestingUserId);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You do not have permission to access this file");
        }
    }

    private static String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }
}
