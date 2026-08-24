package com.attendai.core.file.controller;

import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.file.dto.FileMetadataResponse;
import com.attendai.core.file.dto.FileUploadResponse;
import com.attendai.core.file.dto.PresignedUrlResponse;
import com.attendai.core.file.entity.FileVisibility;
import com.attendai.core.file.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * REST controller for file management.
 * Base path: /api/v1/core/files
 */
@RestController
@RequestMapping("/api/v1/core/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * POST /api/v1/core/files — Upload a file (multipart/form-data).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CORE_FILE_UPLOAD')")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam(name = "file")       MultipartFile   file,
            @RequestParam(name = "visibility", required = false,
                          defaultValue = "PRIVATE") FileVisibility visibility,
            @RequestParam(name = "module",     required = false) String module) {

        Long userId = SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        FileUploadResponse response = fileService.upload(file, visibility, module, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/core/files/{id} — Get file metadata (no binary content).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getMetadata(
            @PathVariable("id") Long id) {

        Long userId  = SecurityContextUtils.getCurrentUserId().orElse(null);
        boolean admin = SecurityContextUtils.hasAuthority("CORE_FILE_READ");

        return ResponseEntity.ok(ApiResponse.success(
                fileService.getMetadata(id, userId, admin)));
    }

    /**
     * GET /api/v1/core/files/{id}/download — Stream file binary content.
     * Public files are accessible without authentication.
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") Long id,
                         HttpServletResponse response) throws Exception {

        Long userId  = SecurityContextUtils.getCurrentUserId().orElse(null);
        boolean admin = SecurityContextUtils.hasAuthority("CORE_FILE_READ");

        // getMetadata is called first to trigger access check and to read content-type
        FileMetadataResponse meta = fileService.getMetadata(id, userId, admin);

        response.setContentType(meta.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + meta.getOriginalName() + "\"");
        response.setContentLengthLong(meta.getSizeBytes());

        try (InputStream in  = fileService.download(id, userId, admin);
             OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }

    /**
     * GET /api/v1/core/files/{id}/presigned-url — Generate time-limited URL.
     */
    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> presignedUrl(
            @PathVariable("id")                     Long id,
            @RequestParam(name = "ttl", required = false) Long ttl) {

        Long userId  = SecurityContextUtils.getCurrentUserId().orElse(null);
        boolean admin = SecurityContextUtils.hasAuthority("CORE_FILE_READ");

        return ResponseEntity.ok(ApiResponse.success(
                fileService.generatePresignedUrl(id, ttl, userId, admin)));
    }

    /**
     * DELETE /api/v1/core/files/{id} — Soft-delete a file.
     * Owner or CORE_FILE_DELETE permission required.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable("id") Long id) {
        Long userId  = SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        boolean admin = SecurityContextUtils.hasAuthority("CORE_FILE_DELETE");

        fileService.deleteFile(id, userId, admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/core/files — List own uploaded files.
     */
    @GetMapping
    public ResponseEntity<PageResponse<FileMetadataResponse>> listOwnFiles(
            @RequestParam(name = "module", required = false) String module,
            @Valid PageRequestParams pageParams) {

        Long userId = SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        return ResponseEntity.ok(PageResponse.of(
                fileService.listOwnFiles(userId, module, pageParams.toPageable())));
    }
}
