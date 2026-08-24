package com.attendai.core.file.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ForbiddenException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.file.config.FileProperties;
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.entity.FileRecord;
import com.attendai.core.file.entity.FileVisibility;
import com.attendai.core.file.exception.FileNotFoundException;
import com.attendai.core.file.exception.FileTypeNotAllowedException;
import com.attendai.core.file.mapper.FileMapper;
import com.attendai.core.file.repository.FileRecordRepository;
import com.attendai.core.file.storage.FileStorageBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock FileRecordRepository fileRecordRepository;
    @Mock FileStorageBackend   fileStorageBackend;
    @Mock FileMapper           fileMapper;
    @Mock AuditService         auditService;

    private FileProperties   fileProperties;
    private FileServiceImpl  fileService;

    @BeforeEach
    void setUp() {
        fileProperties = new FileProperties();
        fileProperties.setAllowedTypes(List.of("image/jpeg", "image/png", "application/pdf"));
        fileProperties.setMaxSizeBytes(10_485_760L);
        fileProperties.setLocalBasePath("/tmp/test");

        fileService = new FileServiceImpl(
                fileRecordRepository, fileStorageBackend,
                fileProperties, fileMapper, auditService);
    }

    // -------------------------------------------------------------------------
    // upload
    // -------------------------------------------------------------------------

    @Test
    void upload_shouldSaveMetadataAndStoreFile_whenValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        FileRecord saved = buildRecord(1L, FileVisibility.PRIVATE, 1L);
        when(fileStorageBackend.store(any(), anyString(), anyString(), anyLong()))
                .thenReturn("core/2025/01/01/uuid.jpg");
        when(fileRecordRepository.save(any())).thenReturn(saved);
        when(fileMapper.toUploadResponse(saved)).thenReturn(FileUploadResponse.builder()
                .id(1L).originalName("photo.jpg").contentType("image/jpeg")
                .sizeBytes(16).visibility(FileVisibility.PRIVATE).build());

        FileUploadResponse result = fileService.upload(file, FileVisibility.PRIVATE, "core-face", 1L);

        assertThat(result.getId()).isEqualTo(1L);
        verify(fileStorageBackend).store(any(), anyString(), anyString(), anyLong());
        verify(fileRecordRepository).save(any(FileRecord.class));
        verify(auditService).log(any());
    }

    @Test
    void upload_shouldThrow400_whenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream",
                "bad-bytes".getBytes());

        assertThatThrownBy(() -> fileService.upload(file, FileVisibility.PRIVATE, null, 1L))
                .isInstanceOf(FileTypeNotAllowedException.class);

        verify(fileStorageBackend, never()).store(any(), any(), any(), anyLong());
        verify(fileRecordRepository, never()).save(any());
    }

    @Test
    void upload_shouldThrow400_whenFileTooLarge() {
        // Set a very small max to trigger the check
        fileProperties.setMaxSizeBytes(5L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", "123456789".getBytes());

        assertThatThrownBy(() -> fileService.upload(file, FileVisibility.PRIVATE, null, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum allowed size");
    }

    // -------------------------------------------------------------------------
    // getMetadata / access control
    // -------------------------------------------------------------------------

    @Test
    void getMetadata_shouldReturn_whenOwner() {
        FileRecord record = buildRecord(1L, FileVisibility.PRIVATE, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(fileMapper.toMetadataResponse(record)).thenReturn(null);

        fileService.getMetadata(1L, 5L, false); // userId=5 = owner, not admin

        verify(fileMapper).toMetadataResponse(record);
    }

    @Test
    void getMetadata_shouldReturn_whenAdmin() {
        FileRecord record = buildRecord(1L, FileVisibility.PRIVATE, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(fileMapper.toMetadataResponse(record)).thenReturn(null);

        fileService.getMetadata(1L, 99L, true); // userId=99, not owner, but admin

        verify(fileMapper).toMetadataResponse(record);
    }

    @Test
    void getMetadata_shouldReturn_whenPublicFile_withNoAuth() {
        FileRecord record = buildRecord(1L, FileVisibility.PUBLIC, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(fileMapper.toMetadataResponse(record)).thenReturn(null);

        fileService.getMetadata(1L, null, false); // no userId, not admin, PUBLIC file

        verify(fileMapper).toMetadataResponse(record);
    }

    @Test
    void getMetadata_shouldThrow403_whenPrivateFileAndNotOwnerOrAdmin() {
        FileRecord record = buildRecord(1L, FileVisibility.PRIVATE, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> fileService.getMetadata(1L, 99L, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getMetadata_shouldThrow404_whenFileNotFound() {
        when(fileRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getMetadata(99L, 1L, true))
                .isInstanceOf(FileNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deleteFile
    // -------------------------------------------------------------------------

    @Test
    void deleteFile_shouldSoftDelete_whenOwner() {
        FileRecord record = buildRecord(1L, FileVisibility.PRIVATE, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(fileRecordRepository.save(any())).thenReturn(record);

        fileService.deleteFile(1L, 5L, false);

        assertThat(record.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    @Test
    void deleteFile_shouldSoftDelete_whenAdmin() {
        FileRecord record = buildRecord(1L, FileVisibility.PRIVATE, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(fileRecordRepository.save(any())).thenReturn(record);

        fileService.deleteFile(1L, 99L, true); // admin, not owner

        assertThat(record.isDeleted()).isTrue();
    }

    @Test
    void deleteFile_shouldThrow403_whenNotOwnerAndNotAdmin() {
        FileRecord record = buildRecord(1L, FileVisibility.PRIVATE, 5L);
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> fileService.deleteFile(1L, 99L, false))
                .isInstanceOf(ForbiddenException.class);
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    void existsById_shouldReturnTrue_whenFileExists() {
        when(fileRecordRepository.existsById(1L)).thenReturn(true);
        assertThat(fileService.existsById(1L)).isTrue();
    }

    @Test
    void existsById_shouldReturnFalse_whenNotFound() {
        when(fileRecordRepository.existsById(99L)).thenReturn(false);
        assertThat(fileService.existsById(99L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FileRecord buildRecord(Long id, FileVisibility visibility, Long ownerId) {
        FileRecord r = FileRecord.builder()
                .originalName("test.jpg")
                .storageKey("core/2025/01/01/uuid.jpg")
                .contentType("image/jpeg")
                .sizeBytes(1024L)
                .visibility(visibility)
                .uploadedByUserId(ownerId)
                .module("core")
                .build();
        r.setId(id);
        return r;
    }
}
