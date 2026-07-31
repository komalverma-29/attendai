# Specification: core-face

## 1. Overview

`core-face` manages face profiles for persons registered on the AttendAI platform and provides the integration interface for the face recognition engine. It handles face image enrollment, face profile lifecycle, and the recognition query API used by attendance stations to identify persons from captured face images.

`core-face` is designed as a domain-agnostic capability. It does not know whether a face belongs to a student, teacher, or employee. It knows only that a face belongs to a `Person`.

The actual AI/ML face recognition computation is delegated to an external or embedded recognition engine behind a Java service interface (`FaceRecognitionEngine`). The concrete implementation is swappable without affecting any other part of the system.

---

## 2. Scope and Objectives

**In scope:**
- Face profile creation (enrollment) for a person
- Adding face images to an existing profile
- Face profile status management (ACTIVE, INACTIVE, PENDING)
- Deleting face images from a profile
- Soft-deleting a face profile
- Face recognition query: given a captured image, return the best-matching person
- Face liveness detection flag (whether liveness check is performed)
- Face profile retrieval by person ID
- Internal recognition API used by `core-attendance` and `core-station`

**Out of scope:**
- Actual AI model training or fine-tuning
- Video stream processing (V1 processes single images only)
- Biometric data storage regulations compliance beyond the defined data model
- Device-side face capture (handled by the station client)

---

## 3. Functional Requirements

### FR-FACE-01: Create Face Profile
Create a face profile for a given person. A person can have only one active face profile. Creating a second profile is rejected unless the existing profile is deactivated or deleted.

### FR-FACE-02: Add Face Image to Profile
Upload one or more face images to an existing profile. Images are stored via `core-file`. The recognition engine processes the image and stores the extracted feature vector (embedding) alongside the file reference. A profile should have at least 1 and at most 10 enrolled images.

### FR-FACE-03: Remove Face Image from Profile
Remove a specific face image from a profile. If the removal would leave the profile with 0 images, the profile status is automatically set to `PENDING` (not ready for recognition).

### FR-FACE-04: Get Face Profile by Person ID
Retrieve the active face profile for a person, including enrolled image count and profile status.

### FR-FACE-05: Get Face Profile by ID
Retrieve a face profile by its surrogate ID.

### FR-FACE-06: Activate Face Profile
Change profile status from `PENDING` or `INACTIVE` to `ACTIVE`. A profile can only be activated if it has at least one enrolled face image.

### FR-FACE-07: Deactivate Face Profile
Change profile status from `ACTIVE` to `INACTIVE`. The person will not be recognized while their profile is inactive.

### FR-FACE-08: Delete Face Profile (Soft)
Soft-delete a face profile and all its associated face images. The person can no longer be identified via face recognition.

### FR-FACE-09: Face Recognition Query
Given a captured face image (submitted as a binary payload or file reference), the system returns the best-matching person (if confidence exceeds the threshold) or indicates no match. Used by `core-attendance` when processing station check-ins.

### FR-FACE-10: Bulk Recognition
Accept multiple face images in a single request and return matches for each. Used by batch station processing.

---

## 4. Non-Functional Requirements

- Face recognition queries must respond within 2 seconds under normal load (single image).
- Enrolled face images are stored in `core-file`. The face module stores only the file ID and the extracted embedding vector.
- Embedding vectors are stored as binary blobs or JSON arrays in the database.
- The recognition engine interface is defined as a Spring service interface, allowing the implementation to be changed (local model, cloud API) without modifying `core-face`.
- A minimum confidence threshold for a positive match is configurable (default: 0.85).
- Liveness detection is optional per-station configuration.

---

## 5. Business Rules

- BR-FACE-01: A person may have at most one active (non-deleted) face profile.
- BR-FACE-02: A face profile must have at least one face image to be in `ACTIVE` status.
- BR-FACE-03: A profile is automatically set to `PENDING` status when created (no images yet).
- BR-FACE-04: Minimum enrolled images for activation: 1. Recommended: 3–5 from different angles.
- BR-FACE-05: Maximum enrolled images per profile: 10.
- BR-FACE-06: A recognition query with confidence below the threshold returns a `NO_MATCH` result.
- BR-FACE-07: Face images are processed (embedding extracted) on upload. The raw image file is stored in `core-file` but the embedding is stored locally in `face_images`.
- BR-FACE-08: Removing the last image from a profile automatically sets status to `PENDING`.

---

## 6. Face Profile Status State Machine

```
[Created] → PENDING
PENDING   → ACTIVE       (activate, requires ≥1 image)
ACTIVE    → INACTIVE     (deactivate)
INACTIVE  → ACTIVE       (re-activate, requires ≥1 image)
ACTIVE    → PENDING      (last image removed)
Any       → [Deleted]    (soft delete)
```

---

## 7. Domain Model

### FaceProfile Entity

| Field       | Type             | Description                                            |
|-------------|------------------|--------------------------------------------------------|
| id          | Long             | Surrogate PK                                           |
| personId    | Long             | FK → persons(id), NOT NULL                             |
| status      | FaceProfileStatus| Enum: PENDING, ACTIVE, INACTIVE                        |
| imageCount  | int              | Denormalized count of active face images               |
| notes       | String           | Optional notes about the profile                       |
| isDeleted   | boolean          | Soft delete flag                                       |
| deletedAt   | LocalDateTime    | Soft delete timestamp                                  |
| createdAt   | LocalDateTime    | Audit                                                  |
| updatedAt   | LocalDateTime    | Audit                                                  |
| createdBy   | Long             | Audit                                                  |
| updatedBy   | Long             | Audit                                                  |

### FaceImage Entity

| Field           | Type          | Description                                          |
|-----------------|---------------|------------------------------------------------------|
| id              | Long          | Surrogate PK                                         |
| faceProfileId   | Long          | FK → face_profiles(id), NOT NULL                     |
| fileId          | Long          | FK → files(id) via core-file, NOT NULL               |
| embeddingVector | String        | JSON array of float values (feature vector), NOT NULL|
| capturedAt      | LocalDateTime | When the image was originally captured, nullable     |
| isDeleted       | boolean       | Soft delete flag                                     |
| deletedAt       | LocalDateTime | Soft delete timestamp                                |
| createdAt       | LocalDateTime | Audit                                                |
| updatedAt       | LocalDateTime | Audit                                                |
| createdBy       | Long          | Audit                                                |
| updatedBy       | Long          | Audit                                                |

### FaceProfileStatus Enum
- `PENDING` — profile created, not yet ready for recognition
- `ACTIVE` — ready for recognition
- `INACTIVE` — temporarily disabled

---

## 8. Entity Relationships

```
persons (core-person)
    │
    │ 1:0..1  (one active face profile per person)
    ▼
face_profiles (core-face)
    │
    │ 1:N
    ▼
face_images (core-face)
    │
    │ N:1
    ▼
files (core-file)
```

---

## 9. Data Model

### Table: `face_profiles`

```sql
CREATE TABLE face_profiles (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    person_id   BIGINT UNSIGNED  NOT NULL,
    status      VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    image_count INT              NOT NULL DEFAULT 0,
    notes       TEXT             NULL,
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_face_profiles_person FOREIGN KEY (person_id) REFERENCES persons(id),
    INDEX idx_face_profiles_person_id (person_id),
    INDEX idx_face_profiles_status (status)
);
```

### Table: `face_images`

```sql
CREATE TABLE face_images (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    face_profile_id  BIGINT UNSIGNED  NOT NULL,
    file_id          BIGINT UNSIGNED  NOT NULL,
    embedding_vector TEXT             NOT NULL,
    captured_at      DATETIME         NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_face_images_profile FOREIGN KEY (face_profile_id) REFERENCES face_profiles(id),
    INDEX idx_face_images_profile_id (face_profile_id)
);
```

---

## 10. Face Recognition Engine Interface

The face recognition capability is abstracted behind a Spring service interface:

```
interface FaceRecognitionEngine {
    FaceEmbedding extractEmbedding(byte[] imageBytes): FaceEmbedding
    RecognitionResult findBestMatch(byte[] imageBytes, List<FaceEmbedding> candidates): RecognitionResult
    boolean isLive(byte[] imageBytes): boolean
}
```

**`FaceEmbedding`** — holds a float array (the feature vector) and metadata.

**`RecognitionResult`** — holds:
- `matched`: boolean
- `personId`: Long (null if no match)
- `confidence`: float (0.0–1.0)
- `faceProfileId`: Long (null if no match)

The concrete implementation class (e.g., `DeepFaceFaceRecognitionEngine`, `AwsRekognitionFaceRecognitionEngine`) is injected by Spring. The interface is the only contract `core-face` depends on.

---

## 11. Package Organization

```
com.attendai.core.face
├── entity
│   ├── FaceProfile.java
│   ├── FaceImage.java
│   └── FaceProfileStatus.java
├── repository
│   ├── FaceProfileRepository.java
│   └── FaceImageRepository.java
├── service
│   ├── FaceService.java
│   ├── FaceServiceImpl.java
│   └── FaceRecognitionEngine.java          ← interface
├── controller
│   └── FaceController.java
├── dto
│   ├── CreateFaceProfileRequest.java
│   ├── AddFaceImageRequest.java
│   ├── FaceProfileResponse.java
│   ├── FaceImageResponse.java
│   ├── RecognitionRequest.java
│   └── RecognitionResponse.java
├── mapper
│   └── FaceMapper.java
└── exception
    ├── FaceProfileNotFoundException.java
    └── FaceProfileAlreadyExistsException.java
```

---

## 12. API Contracts

Base path: `/api/v1/core/face`

### POST /api/v1/core/face/profiles — Create Face Profile

**Permission:** `CORE_FACE_ENROLL`

**Request:**
```json
{
  "personId": 101,
  "notes": "Enrolled at main entrance"
}
```

**Response 201:** `FaceProfileResponse`

---

### GET /api/v1/core/face/profiles/{id}

**Permission:** `CORE_FACE_READ`

**Response 200:** `FaceProfileResponse`

---

### GET /api/v1/core/face/profiles/person/{personId}

**Permission:** `CORE_FACE_READ`

**Response 200:** `FaceProfileResponse`
**Response 404:** No active profile for person

---

### POST /api/v1/core/face/profiles/{id}/images — Add Face Image

**Permission:** `CORE_FACE_ENROLL`

**Request:** `multipart/form-data` with field `image` (binary) and optional `capturedAt` (ISO datetime string)

**Response 201:** `FaceImageResponse`

---

### DELETE /api/v1/core/face/profiles/{id}/images/{imageId} — Remove Face Image

**Permission:** `CORE_FACE_ENROLL`

**Response 204**

---

### PATCH /api/v1/core/face/profiles/{id}/activate

**Permission:** `CORE_FACE_ENROLL`

**Response 200:** `FaceProfileResponse`
**Response 400:** Profile has no enrolled images

---

### PATCH /api/v1/core/face/profiles/{id}/deactivate

**Permission:** `CORE_FACE_ENROLL`

**Response 200:** `FaceProfileResponse`

---

### DELETE /api/v1/core/face/profiles/{id}

**Permission:** `CORE_FACE_DELETE`

**Response 204**

---

### POST /api/v1/core/face/recognize — Face Recognition Query

**Permission:** `CORE_FACE_RECOGNIZE` (typically held by station system accounts)

**Request:** `multipart/form-data` with `image` (binary) and optional `livenessCheck` (boolean)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "matched": true,
    "personId": 101,
    "faceProfileId": 5,
    "confidence": 0.97,
    "livenessCheckPassed": true
  }
}
```

**Response 200 (no match):**
```json
{
  "success": true,
  "data": {
    "matched": false,
    "personId": null,
    "faceProfileId": null,
    "confidence": 0.41,
    "livenessCheckPassed": true
  }
}
```

---

## 13. Recognition Flow

```
Station captures image
    │
    ▼
POST /api/v1/core/face/recognize { image }
    │
    ▼
[FaceServiceImpl]
    ├── Optionally: FaceRecognitionEngine.isLive(imageBytes) → reject if not live
    ├── Load all ACTIVE face embeddings from face_images
    ├── FaceRecognitionEngine.findBestMatch(imageBytes, embeddings)
    └── If confidence ≥ threshold → return match (personId, profileId, confidence)
        Else → return no-match result
    │
    ▼
[core-attendance] uses personId to record attendance event
```

---

## 14. Authorization

| Operation              | Required Permission    |
|------------------------|------------------------|
| Create face profile    | `CORE_FACE_ENROLL`     |
| Read face profile      | `CORE_FACE_READ`       |
| Add/remove image       | `CORE_FACE_ENROLL`     |
| Activate/deactivate    | `CORE_FACE_ENROLL`     |
| Delete profile         | `CORE_FACE_DELETE`     |
| Recognition query      | `CORE_FACE_RECOGNIZE`  |

---

## 15. Configuration

Managed via `@ConfigurationProperties(prefix = "attendai.face")`:

| Property                               | Default | Description                              |
|----------------------------------------|---------|------------------------------------------|
| `attendai.face.recognition-threshold`  | `0.85`  | Minimum confidence for a positive match  |
| `attendai.face.max-images-per-profile` | `10`    | Maximum face images per profile          |
| `attendai.face.liveness-check-enabled` | `false` | Enable liveness check globally           |
| `attendai.face.engine`                 | (req'd) | Engine bean name to inject               |

---

## 16. Integration Points

| Module           | Integration                                                     |
|------------------|-----------------------------------------------------------------|
| `core-common`    | `SoftDeletableEntity`, exceptions, response types               |
| `core-person`    | Validates `personId` before creating profile                    |
| `core-file`      | Stores face images via file upload; references `files(id)`     |
| `core-attendance`| Calls recognition API to identify persons at check-in          |
| `core-audit`     | Audit events for profile create, enroll, delete, recognition   |

---

## 17. Error Handling

| Scenario                              | Exception                            | HTTP |
|---------------------------------------|--------------------------------------|------|
| Profile not found                     | `FaceProfileNotFoundException`       | 404  |
| Person already has active profile     | `FaceProfileAlreadyExistsException`  | 409  |
| Activate profile with no images       | `ValidationException`                | 400  |
| Image limit exceeded (>10)            | `ValidationException`                | 400  |
| Recognition engine unavailable        | `ExternalServiceException`           | 502  |
| Invalid image format                  | `ValidationException`                | 400  |
| Person not found                      | `ResourceNotFoundException`          | 404  |

---

## 18. Logging and Audit

| Action                  | Audit Code                  | Details                           |
|-------------------------|-----------------------------|-----------------------------------|
| Profile created         | `FACE_PROFILE_CREATED`      | profile_id, person_id             |
| Image enrolled          | `FACE_IMAGE_ENROLLED`       | profile_id, image_id              |
| Image removed           | `FACE_IMAGE_REMOVED`        | profile_id, image_id              |
| Profile activated       | `FACE_PROFILE_ACTIVATED`    | profile_id                        |
| Profile deactivated     | `FACE_PROFILE_DEACTIVATED`  | profile_id                        |
| Profile deleted         | `FACE_PROFILE_DELETED`      | profile_id, person_id             |
| Recognition match       | `FACE_RECOGNITION_MATCH`    | person_id, confidence, station_id |
| Recognition no-match    | `FACE_RECOGNITION_NO_MATCH` | confidence                        |

---

## 19. Security Considerations

- Face images and embedding vectors are biometric data. Access is restricted to `CORE_FACE_ENROLL` and `CORE_FACE_READ` permissions.
- Embedding vectors in the database must not be exposed through the public API.
- The recognition endpoint is restricted to `CORE_FACE_RECOGNIZE` — typically held only by system/station accounts.
- File references to raw face images use `core-file` access controls.

---

## 20. Performance and Scalability

- Recognition searches all ACTIVE embeddings. For V1 with up to 10,000 enrolled persons, an in-memory cosine similarity search is acceptable.
- Beyond 10,000 persons, an approximate nearest-neighbour index (e.g., FAISS, Annoy) should be used. This is a future scalability enhancement — the `FaceRecognitionEngine` interface accommodates this transparently.
- Embedding vectors are loaded lazily only during recognition queries.

---

## 21. Edge Cases and Failure Scenarios

| Scenario                                         | Handling                                    |
|--------------------------------------------------|---------------------------------------------|
| Recognition engine unavailable                   | Return 502 with `EXTERNAL_SERVICE_ERROR`    |
| Confidence exactly at threshold                  | Treated as a match (threshold is inclusive) |
| Multiple persons at similar confidence           | Return highest confidence match only        |
| Enrolled image file deleted from core-file       | Log warning; image is skipped during recognition |
| Liveness check fails                             | Return `matched: false`, `livenessCheckPassed: false` |
| Image submitted is not a face                    | Engine returns 0.0 confidence → no match    |

---

## 22. Flyway Migrations

```
V11__create_face_profiles_table.sql
V12__create_face_images_table.sql
```

---

## 23. Testing Strategy

| Test Type       | Scope                                                                       |
|-----------------|-----------------------------------------------------------------------------|
| Unit — Service  | Create profile, add image, remove image, status transitions                 |
| Unit — Service  | Duplicate profile rejection, activate-without-images rejection              |
| Unit — Service  | Recognition result: match, no-match, liveness fail                          |
| Mock engine     | `FaceRecognitionEngine` is mocked in all service unit tests                 |
| Repository test | `findByPersonId`, `findActiveByPersonId`, soft-delete filter                |
| Controller test | All endpoints, multipart upload, HTTP codes                                 |
| Integration     | Create profile → enroll image → activate → recognize (using mock engine)    |

---

## 24. Implementation Roadmap

### Task 1: Domain model and migrations
- `FaceProfile`, `FaceImage` entities, `FaceProfileStatus` enum
- `FaceProfileRepository`, `FaceImageRepository`
- Flyway: `V11`, `V12`

### Task 2: Recognition engine interface
- Define `FaceRecognitionEngine` interface and supporting DTOs (`FaceEmbedding`, `RecognitionResult`)
- Create a `MockFaceRecognitionEngine` stub for testing

### Task 3: Service — profile management
- `createProfile`, `findById`, `findByPersonId`, `activateProfile`, `deactivateProfile`, `deleteProfile`
- Status transition validation

### Task 4: Service — image management
- `addFaceImage` (upload → store via core-file → extract embedding → persist)
- `removeFaceImage` (soft-delete image, update `imageCount`, check PENDING trigger)

### Task 5: Service — recognition
- `recognize(imageBytes)`: extract embedding, compare against all ACTIVE embeddings, return best match

### Task 6: Controller and DTOs
- `FaceController` with all endpoints
- Multipart image upload handling
- `FaceMapper`

### Task 7: Configuration
- `FaceProperties` `@ConfigurationProperties` class

### Task 8: Audit integration

---

## 25. Acceptance Criteria

- [ ] A person can have at most one active face profile
- [ ] A profile with no images cannot be activated
- [ ] Removing the last image from a profile sets status to `PENDING`
- [ ] Recognition returns `matched: false` when confidence is below threshold
- [ ] Embedding vectors are not exposed in any API response
- [ ] Recognition engine failures return 502, not 500
- [ ] All face management events are written to the audit log
- [ ] `FaceRecognitionEngine` implementation is swappable via Spring DI without code changes

---

## 26. Out of Scope

- Video stream processing
- Multi-face detection in a single image
- AI model training pipeline
- GDPR biometric data deletion workflows beyond soft delete
- Face anti-spoofing beyond liveness check flag
