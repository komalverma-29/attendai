# Specification: core-file

## 1. Overview

`core-file` is the centralized file management module for the AttendAI platform. It handles file uploads, storage, metadata persistence, access control, and retrieval across all modules.

Any module that needs to store binary data (face images, profile photos, report exports, document attachments) uses `core-file`. The module abstracts the underlying storage backend — local filesystem, S3-compatible object store, or other — behind a `FileStorageBackend` interface, making the storage layer swappable without changing the service API.

`core-file` is domain-agnostic. A file record simply tracks what was uploaded, who uploaded it, its size, its media type, its storage key, and its access visibility.

---

## 2. Scope and Objectives

**In scope:**
- File upload (single file, multipart HTTP)
- File metadata storage (original filename, content type, size, storage key, uploader)
- File download (streaming binary response)
- File deletion (soft delete, with optional physical deletion from storage)
- File access visibility (PRIVATE vs PUBLIC)
- File metadata query by ID
- File validation (content type whitelist, size limits)
- Generating temporary pre-signed download URLs (for private files)

**Out of scope:**
- Document parsing or content indexing
- Image resizing or thumbnail generation (V1)
- Virus scanning (future enhancement)
- Multi-part chunked upload for very large files (V1 uses single-part upload)

---

## 3. Functional Requirements

### FR-FILE-01: Upload File
Accept a multipart file upload. Validate content type against an allowed list and enforce a maximum file size. Generate a unique storage key, delegate storage to the `FileStorageBackend`, and persist metadata.

### FR-FILE-02: Get File Metadata
Retrieve the metadata record for a file by its surrogate ID. Does not return the binary content.

### FR-FILE-03: Download File
Stream the binary file content to the client. Validates that the requesting user is authorized to access the file (owner, `CORE_FILE_READ` permission, or file is PUBLIC).

### FR-FILE-04: Delete File (Soft)
Soft-delete the file metadata record. The physical file in the storage backend may optionally be deleted (configurable). After soft delete, the file is no longer accessible.

### FR-FILE-05: Hard Delete File
Physically delete the file from both the metadata store and the storage backend. Used for cleanup and GDPR erasure. Requires `CORE_FILE_DELETE` permission.

### FR-FILE-06: List Files for Owner
Return a paginated list of files uploaded by a specific user.

### FR-FILE-07: Generate Pre-Signed URL
For PRIVATE files stored in an object store (S3-compatible), generate a time-limited pre-signed URL that allows direct download without going through the API. For local storage, this generates a short-lived signed token URL served through the API.

---

## 4. Non-Functional Requirements

- File upload must stream to the storage backend without loading the entire file into JVM heap (use streaming multipart handling).
- Maximum file size: configurable, default 10 MB.
- Allowed content types: configurable whitelist, default: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`.
- Storage keys are globally unique (UUID-based with directory prefixing).
- The `FileStorageBackend` interface allows hot-swapping storage implementations via Spring configuration.
- Download response must use appropriate `Content-Type` and `Content-Disposition` headers.

---

## 5. Business Rules

- BR-FILE-01: Only the file's uploader or a user with `CORE_FILE_READ` permission may download a PRIVATE file.
- BR-FILE-02: PUBLIC files are accessible without authentication (served as static resources or via unauthenticated download endpoint).
- BR-FILE-03: Content type is validated both from the HTTP `Content-Type` header and by inspecting the file's magic bytes (first bytes). Mismatches are rejected.
- BR-FILE-04: A soft-deleted file cannot be downloaded.
- BR-FILE-05: The original filename is stored for reference but the storage key is UUID-based to prevent path traversal attacks.
- BR-FILE-06: Maximum single file size is enforced at the API layer before the file reaches the service.

---

## 6. Domain Model

### FileRecord Entity

| Field          | Type          | Description                                                    |
|----------------|---------------|----------------------------------------------------------------|
| id             | Long          | Surrogate PK                                                   |
| originalName   | String        | Original filename as uploaded, max 255                         |
| storageKey     | String        | Unique key in the storage backend (UUID path), max 500         |
| contentType    | String        | MIME type, e.g. `image/jpeg`, max 100                          |
| sizeBytes      | Long          | File size in bytes                                             |
| visibility     | FileVisibility| Enum: PRIVATE, PUBLIC                                          |
| uploadedByUserId| Long         | FK → users(id), NOT NULL                                       |
| module         | String        | Module that owns the file, e.g. `core-face`, `school`, max 50  |
| isDeleted      | boolean       | Soft delete flag                                               |
| deletedAt      | LocalDateTime | Soft delete timestamp                                          |
| createdAt      | LocalDateTime | Audit                                                          |
| updatedAt      | LocalDateTime | Audit                                                          |
| createdBy      | Long          | Audit                                                          |
| updatedBy      | Long          | Audit                                                          |

### FileVisibility Enum
- `PRIVATE` — only uploader and privileged users can access
- `PUBLIC` — accessible without authentication

---

## 7. File Storage Backend Interface

```
interface FileStorageBackend {
    String store(InputStream inputStream, String storageKey, String contentType, long sizeBytes): String
      // Stores the file and returns the storage key (confirmed)

    InputStream retrieve(String storageKey): InputStream
      // Returns a stream for the file content

    void delete(String storageKey): void
      // Physically removes the file from storage

    String generatePresignedUrl(String storageKey, int ttlSeconds): String
      // Returns a time-limited URL for direct access
}
```

Concrete implementations:
- `LocalFileStorageBackend` — stores files in a configured directory on the local filesystem
- `S3FileStorageBackend` — stores files in an S3-compatible bucket (AWS S3, MinIO)

The active implementation is selected via `attendai.file.storage.backend` configuration property.

---

## 8. Storage Key Naming Convention

```
<module>/<yyyy>/<MM>/<dd>/<uuid>.<extension>
```

Example:
```
core-face/2025/01/15/a3f9c2d1-4b8e-4f2a-9c3e-1d2f3a4b5c6d.jpg
school/2025/01/15/7e8f9a0b-1c2d-3e4f-5a6b-7c8d9e0f1a2b.pdf
```

This provides natural time-based directory bucketing and avoids filesystem hotspots.

---

## 9. Data Model

### Table: `files`

```sql
CREATE TABLE files (
    id                   BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    original_name        VARCHAR(255)     NOT NULL,
    storage_key          VARCHAR(500)     NOT NULL,
    content_type         VARCHAR(100)     NOT NULL,
    size_bytes           BIGINT UNSIGNED  NOT NULL,
    visibility           VARCHAR(10)      NOT NULL DEFAULT 'PRIVATE',
    uploaded_by_user_id  BIGINT UNSIGNED  NOT NULL,
    module               VARCHAR(50)      NULL,
    is_deleted           BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at           DATETIME         NULL,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT UNSIGNED  NULL,
    updated_by           BIGINT UNSIGNED  NULL,

    UNIQUE uq_files_storage_key (storage_key),
    INDEX idx_files_uploaded_by (uploaded_by_user_id),
    INDEX idx_files_module (module),
    INDEX idx_files_is_deleted (is_deleted)
);
```

---

## 10. Package Organization

```
com.attendai.core.file
├── entity
│   ├── FileRecord.java
│   └── FileVisibility.java
├── repository
│   └── FileRecordRepository.java
├── service
│   ├── FileService.java
│   ├── FileServiceImpl.java
│   └── FileStorageBackend.java          ← interface
├── storage
│   ├── LocalFileStorageBackend.java
│   └── S3FileStorageBackend.java
├── controller
│   └── FileController.java
├── dto
│   ├── FileUploadResponse.java
│   ├── FileMetadataResponse.java
│   └── PresignedUrlResponse.java
├── mapper
│   └── FileMapper.java
├── config
│   └── FileStorageConfig.java
└── exception
    ├── FileNotFoundException.java
    └── FileTypeNotAllowedException.java
```

---

## 11. API Contracts

Base path: `/api/v1/core/files`

### POST /api/v1/core/files — Upload File

**Permission:** `CORE_FILE_UPLOAD`

**Request:** `multipart/form-data`
- `file`: binary file part (required)
- `visibility`: `PRIVATE` or `PUBLIC` (optional, default `PRIVATE`)
- `module`: module namespace string (optional, max 50)

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": 200,
    "originalName": "john-doe-face.jpg",
    "contentType": "image/jpeg",
    "sizeBytes": 102400,
    "visibility": "PRIVATE",
    "module": "core-face",
    "createdAt": "2025-01-15T10:00:00Z"
  }
}
```

`storageKey` is never included in the response. It is an internal storage detail.

---

### GET /api/v1/core/files/{id} — Get File Metadata

**Permission:** `CORE_FILE_READ` or file owner

**Response 200:** `FileMetadataResponse`

---

### GET /api/v1/core/files/{id}/download — Download File

**Permission:** `CORE_FILE_READ` or file owner (PRIVATE); no auth required (PUBLIC)

**Response 200:** Binary stream
- `Content-Type: <file content type>`
- `Content-Disposition: attachment; filename="<originalName>"`

**Response 404:** File not found or soft-deleted
**Response 403:** Caller does not have access to PRIVATE file

---

### GET /api/v1/core/files/{id}/presigned-url — Generate Pre-Signed URL

**Permission:** `CORE_FILE_READ` or file owner

**Query params:** `ttl` (seconds, default 300, max 3600)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "url": "https://storage.example.com/...",
    "expiresAt": "2025-01-15T10:05:00Z"
  }
}
```

---

### DELETE /api/v1/core/files/{id} — Soft Delete File

**Permission:** `CORE_FILE_DELETE` or file owner

**Response 204**

---

### GET /api/v1/core/files — List Own Files

**Authentication:** Any authenticated user (own files only unless `CORE_FILE_READ`)

**Query params:** `page`, `size`, `module`

**Response 200:** Paginated `FileMetadataResponse`

---

## 12. Validation Rules

### File Upload Validation
| Constraint     | Rule                                             |
|----------------|--------------------------------------------------|
| File size      | Max configurable (default 10 MB)                 |
| Content type   | Must be in the allowed whitelist                 |
| Magic bytes    | First bytes inspected to confirm declared type   |
| File name      | Max 255 chars; path separators (`/`, `\`) stripped|
| Visibility     | Optional, default PRIVATE                        |

---

## 13. Authorization

| Operation          | Required                                    |
|--------------------|---------------------------------------------|
| Upload file        | `CORE_FILE_UPLOAD`                          |
| Get metadata       | `CORE_FILE_READ` or own file                |
| Download PRIVATE   | `CORE_FILE_READ` or own file                |
| Download PUBLIC    | No authentication required                  |
| Generate presigned | `CORE_FILE_READ` or own file                |
| Soft delete        | `CORE_FILE_DELETE` or own file              |
| List own files     | Any authenticated user                      |

---

## 14. Internal Service API

Exposed as Spring beans:

```
FileService.getStorageKey(Long fileId): String
FileService.existsById(Long fileId): boolean
FileService.deleteById(Long fileId): void
FileService.retrieveStream(Long fileId): InputStream
```

Used by:
- `core-face` — stores face image files; retrieves them for recognition
- `core-person` — references profile photo file ID
- Business modules — for document attachments, exports

---

## 15. Configuration

| Property                               | Default                                       | Description                          |
|----------------------------------------|-----------------------------------------------|--------------------------------------|
| `attendai.file.storage.backend`        | `local`                                       | `local` or `s3`                      |
| `attendai.file.storage.local.base-path`| `/var/attendai/files`                         | Local storage root directory         |
| `attendai.file.storage.s3.bucket`      | (required if s3)                              | S3 bucket name                       |
| `attendai.file.storage.s3.region`      | (required if s3)                              | AWS region                           |
| `attendai.file.max-size-bytes`         | `10485760` (10 MB)                            | Maximum file size                    |
| `attendai.file.allowed-types`          | `image/jpeg,image/png,image/webp,application/pdf` | Comma-separated MIME whitelist   |
| `attendai.file.presigned-url.max-ttl`  | `3600`                                        | Max TTL for pre-signed URLs (seconds)|

---

## 16. Integration Points

| Module           | Integration                                                       |
|------------------|-------------------------------------------------------------------|
| `core-common`    | `SoftDeletableEntity`, exceptions, response types                 |
| `core-face`      | Stores face images; retrieves embedding source files              |
| `core-person`    | Profile photo `profile_photo_file_id` references `files(id)`     |
| `core-audit`     | Audit events for file upload, download, delete                    |

---

## 17. Error Handling

| Scenario                       | Exception                       | HTTP |
|--------------------------------|---------------------------------|------|
| File not found                 | `FileNotFoundException`         | 404  |
| Content type not allowed       | `FileTypeNotAllowedException`   | 400  |
| File too large                 | Standard Spring exception       | 413  |
| Storage backend unavailable    | `ExternalServiceException`      | 502  |
| Access to PRIVATE file denied  | `ForbiddenException`            | 403  |
| Pre-signed URL for local backend| Return API-proxied download URL |      |

---

## 18. Security Considerations

- `storageKey` is never exposed in any API response. This prevents direct storage access attempts.
- Original filenames are sanitized: path separators stripped, length enforced.
- Content type is validated from both the HTTP header and file magic bytes to prevent extension spoofing.
- PRIVATE files require ownership or explicit permission to download.
- PUBLIC files should only be used for genuinely public content (e.g., platform logos). No personal or sensitive data should be stored as PUBLIC.
- Pre-signed URLs have a maximum TTL of 1 hour.

---

## 19. Logging and Audit

| Action           | Audit Code         | Details                         |
|------------------|--------------------|---------------------------------|
| File uploaded    | `FILE_UPLOADED`    | file_id, module, size_bytes     |
| File downloaded  | `FILE_DOWNLOADED`  | file_id, user_id                |
| File deleted     | `FILE_DELETED`     | file_id                         |

---

## 20. Flyway Migrations

```
V21__create_files_table.sql
```

---

## 21. Testing Strategy

| Test Type       | Scope                                                                 |
|-----------------|-----------------------------------------------------------------------|
| Unit — Service  | Upload: valid file, invalid type, size exceeded                       |
| Unit — Service  | Download: PRIVATE access check, soft-deleted rejection                |
| Unit — Service  | `LocalFileStorageBackend`: store, retrieve, delete                    |
| Mock backend    | `FileStorageBackend` is mocked in service unit tests                  |
| Repository test | `findByUploadedByUserId`, soft-delete exclusion                       |
| Controller test | Upload endpoint (multipart), download stream, metadata endpoint       |
| Security tests  | PRIVATE download rejected without auth; PUBLIC accessible             |
| Integration     | Upload → metadata stored → download returns correct bytes             |

---

## 22. Implementation Roadmap

### Task 1: Entity, migration, repository
- `FileRecord` entity, `FileVisibility` enum
- `FileRecordRepository`
- Flyway: `V21__create_files_table.sql`

### Task 2: Storage backend interface and local implementation
- `FileStorageBackend` interface
- `LocalFileStorageBackend`: store to configurable directory, retrieve stream, delete, generate signed token URL

### Task 3: Core service
- `uploadFile`: validate type, validate size, generate storage key, delegate to backend, persist metadata
- `downloadFile`: access check, retrieve stream, set headers
- `getMetadata`, `deleteFile`, `listOwnFiles`
- `generatePresignedUrl`

### Task 4: Controller and DTOs
- `FileController` with all endpoints
- Multipart upload handling
- Streaming download response

### Task 5: S3 backend implementation (optional V1)
- `S3FileStorageBackend`

### Task 6: Configuration class
- `FileProperties` `@ConfigurationProperties`

### Task 7: Audit integration

---

## 23. Acceptance Criteria

- [ ] Files with disallowed content types are rejected with 400
- [ ] Files exceeding the configured size limit are rejected with 413
- [ ] `storageKey` never appears in any API response
- [ ] PRIVATE file download returns 403 for users without ownership or `CORE_FILE_READ`
- [ ] PUBLIC file download succeeds without authentication
- [ ] Soft-deleted files return 404 on download
- [ ] `FileStorageBackend` implementation is swappable via configuration without code changes
- [ ] Magic byte validation catches extension-spoofed files

---

## 24. Out of Scope

- Image resizing and thumbnail generation
- Virus / malware scanning
- Chunked multipart upload for files >10 MB
- Document text extraction or OCR
- CDN integration
- File versioning
