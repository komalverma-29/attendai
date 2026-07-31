# Specification: core-person

## 1. Overview

`core-person` manages the generic person entity — the foundational human identity record in the AttendAI platform. A person represents a real human being with identity, contact, and basic demographic information.

A person is domain-agnostic. It does not know whether it belongs to a school student, a college faculty member, or an enterprise employee. Business modules extend person data by creating their own domain entities that hold a foreign key reference to `persons.id`.

Every authenticated user is backed by a person record. A person may also exist independently of a user (e.g., a person whose face is registered at a station but who has no login access).

`core-person` is a fundamental dependency of `core-user`, `core-face`, `core-attendance`, and all business modules.

---

## 2. Scope and Objectives

**In scope:**
- Person record CRUD (create, read, update, soft-delete)
- Storing generic identity fields: name, gender, date of birth, contact details, identity document reference
- Profile photo reference (links to a file managed by `core-file`)
- Person search by name, email, phone
- Validation of person data
- Soft delete with referential integrity enforcement

**Out of scope:**
- Domain-specific person roles (student, teacher, employee — defined in business modules)
- User account creation (belongs in `core-user`)
- Face profile management (belongs in `core-face`)
- Attendance records (belongs in `core-attendance`)

---

## 3. Functional Requirements

### FR-PERSON-01: Create Person
Create a new person record with required fields: first name, last name. Optional fields: middle name, gender, date of birth, email, phone, address, identity document type and number, profile photo file reference.

### FR-PERSON-02: Get Person by ID
Retrieve a single non-deleted person by their surrogate ID.

### FR-PERSON-03: Search Persons
Return a paginated list of persons, searchable by full name (partial match), email, or phone number.

### FR-PERSON-04: Update Person
Update permitted person fields. All fields except `id` and `createdAt` can be updated.

### FR-PERSON-05: Delete Person (Soft)
Soft-delete a person record. A person cannot be deleted if they have an active (non-deleted) user account linked to them.

### FR-PERSON-06: Get Person by User ID
Given a user ID, return the associated person record. Used internally by other modules.

### FR-PERSON-07: Verify Person Exists
A lightweight existence check by ID, used by other modules (e.g., `core-user` before creating a user account, `core-face` before creating a face profile).

---

## 4. Non-Functional Requirements

- Email address on a person is not required (persons may exist without email).
- A person's email must be unique across non-deleted persons if provided.
- Person search must support at minimum 10,000 person records with sub-100ms response times on indexed columns.
- The `persons` table is a foundational table. All FK references to `persons(id)` from other modules use `ON DELETE RESTRICT`.

---

## 5. Business Rules

- BR-PERSON-01: First name and last name are required.
- BR-PERSON-02: Email, if provided, must be a valid format and unique among non-deleted persons.
- BR-PERSON-03: Date of birth, if provided, must be a date in the past.
- BR-PERSON-04: A person with an active user account cannot be soft-deleted.
- BR-PERSON-05: A person with an active face profile cannot be soft-deleted.
- BR-PERSON-06: Profile photo file ID, if set, must reference a valid file in `core-file`.
- BR-PERSON-07: Identity document number, if provided, is stored as-is. No format enforcement (formats vary globally).

---

## 6. Domain Model

### Person Entity

| Field               | Type          | Description                                              |
|---------------------|---------------|----------------------------------------------------------|
| id                  | Long          | Surrogate PK                                             |
| firstName           | String        | NOT NULL, max 100                                        |
| middleName          | String        | Optional, max 100                                        |
| lastName            | String        | NOT NULL, max 100                                        |
| gender              | Gender        | Enum: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY; nullable   |
| dateOfBirth         | LocalDate     | Nullable, must be past                                   |
| email               | String        | Optional, unique among non-deleted, max 255              |
| phone               | String        | Optional, max 30                                         |
| addressLine1        | String        | Optional, max 255                                        |
| addressLine2        | String        | Optional, max 255                                        |
| city                | String        | Optional, max 100                                        |
| stateOrProvince     | String        | Optional, max 100                                        |
| postalCode          | String        | Optional, max 20                                         |
| country             | String        | Optional, ISO 3166-1 alpha-2, max 2                      |
| identityDocType     | String        | Optional: PASSPORT, NATIONAL_ID, DRIVING_LICENCE, OTHER  |
| identityDocNumber   | String        | Optional, max 100                                        |
| profilePhotoFileId  | Long          | Optional, FK → files(id) via core-file                  |
| isDeleted           | boolean       | Soft delete flag                                         |
| deletedAt           | LocalDateTime | Soft delete timestamp                                    |
| createdAt           | LocalDateTime | Audit                                                    |
| updatedAt           | LocalDateTime | Audit                                                    |
| createdBy           | Long          | Audit                                                    |
| updatedBy           | Long          | Audit                                                    |

### Gender Enum
- `MALE`
- `FEMALE`
- `OTHER`
- `PREFER_NOT_TO_SAY`

### IdentityDocType (stored as VARCHAR)
- `PASSPORT`
- `NATIONAL_ID`
- `DRIVING_LICENCE`
- `OTHER`

---

## 7. Entity Relationships

```
persons (core-person)
    │
    ├── 1:0..1  users (core-user)            [person_id FK on users]
    ├── 1:0..N  face_profiles (core-face)    [person_id FK on face_profiles]
    ├── 1:0..N  attendance_events (core-attendance) [person_id FK on attendance_events]
    └── 1:0..N  <business module entities>   [person_id FK, defined in domain module]
```

Core never references the domain-module entities. The FK always points inward toward `persons`.

---

## 8. Data Model

### Table: `persons`

```sql
CREATE TABLE persons (
    id                   BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    first_name           VARCHAR(100)     NOT NULL,
    middle_name          VARCHAR(100)     NULL,
    last_name            VARCHAR(100)     NOT NULL,
    gender               VARCHAR(20)      NULL,
    date_of_birth        DATE             NULL,
    email                VARCHAR(255)     NULL,
    phone                VARCHAR(30)      NULL,
    address_line1        VARCHAR(255)     NULL,
    address_line2        VARCHAR(255)     NULL,
    city                 VARCHAR(100)     NULL,
    state_or_province    VARCHAR(100)     NULL,
    postal_code          VARCHAR(20)      NULL,
    country              VARCHAR(2)       NULL,
    identity_doc_type    VARCHAR(30)      NULL,
    identity_doc_number  VARCHAR(100)     NULL,
    profile_photo_file_id BIGINT UNSIGNED NULL,
    is_deleted           BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at           DATETIME         NULL,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT UNSIGNED  NULL,
    updated_by           BIGINT UNSIGNED  NULL,

    UNIQUE uq_persons_email (email),
    INDEX idx_persons_last_name (last_name),
    INDEX idx_persons_email (email),
    INDEX idx_persons_phone (phone),
    INDEX idx_persons_is_deleted (is_deleted)
);
```

The `UNIQUE uq_persons_email` constraint applies to all records including soft-deleted ones. This is intentional to prevent email reuse after soft deletion (consistent with `core-user` email uniqueness policy).

---

## 9. Package Organization

```
com.attendai.core.person
├── entity
│   ├── Person.java
│   ├── Gender.java
│   └── IdentityDocType.java
├── repository
│   └── PersonRepository.java
├── service
│   ├── PersonService.java
│   └── PersonServiceImpl.java
├── controller
│   └── PersonController.java
├── dto
│   ├── CreatePersonRequest.java
│   ├── UpdatePersonRequest.java
│   ├── PersonResponse.java
│   └── PersonSummaryResponse.java
├── mapper
│   └── PersonMapper.java
└── exception
    └── PersonNotFoundException.java
```

---

## 10. API Contracts

Base path: `/api/v1/core/persons`

### POST /api/v1/core/persons — Create Person

**Permission:** `CORE_PERSON_CREATE`

**Request:**
```json
{
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "gender": "MALE",
  "dateOfBirth": "1990-05-15",
  "email": "john.doe@example.com",
  "phone": "+91-9876543210",
  "addressLine1": "123 Main Street",
  "city": "Mumbai",
  "stateOrProvince": "Maharashtra",
  "postalCode": "400001",
  "country": "IN",
  "identityDocType": "PASSPORT",
  "identityDocNumber": "A1234567"
}
```

**Response 201:** `PersonResponse`

---

### GET /api/v1/core/persons/{id}

**Permission:** `CORE_PERSON_READ`

**Response 200:** `PersonResponse`
**Response 404:** Person not found

---

### GET /api/v1/core/persons — Search/List Persons

**Permission:** `CORE_PERSON_READ`

**Query params:** `page`, `size`, `search` (matches first name, last name, email, phone)

**Response 200:** Paginated `PersonSummaryResponse`

---

### PUT /api/v1/core/persons/{id} — Update Person

**Permission:** `CORE_PERSON_UPDATE`

**Request:** Same shape as `CreatePersonRequest` (all fields optional on update)

**Response 200:** `PersonResponse`

---

### DELETE /api/v1/core/persons/{id} — Soft Delete

**Permission:** `CORE_PERSON_DELETE`

**Response 204**
**Response 409:** Person has an active user account or face profile

---

## 11. Request Validation Rules

### CreatePersonRequest
| Field              | Rule                                               |
|--------------------|----------------------------------------------------|
| firstName          | Not blank, max 100                                 |
| lastName           | Not blank, max 100                                 |
| middleName         | Optional, max 100                                  |
| gender             | Optional, valid `Gender` enum value                |
| dateOfBirth        | Optional, must be a past date                      |
| email              | Optional, valid email format, max 255, unique       |
| phone              | Optional, max 30                                   |
| addressLine1       | Optional, max 255                                  |
| city               | Optional, max 100                                  |
| stateOrProvince    | Optional, max 100                                  |
| postalCode         | Optional, max 20                                   |
| country            | Optional, ISO 3166-1 alpha-2, exactly 2 chars      |
| identityDocType    | Optional, valid enum value                         |
| identityDocNumber  | Optional, max 100                                  |
| profilePhotoFileId | Optional, must reference valid file if provided    |

---

## 12. Response Models

### PersonResponse (full)
```json
{
  "id": 101,
  "firstName": "John",
  "middleName": "Michael",
  "lastName": "Doe",
  "fullName": "John Michael Doe",
  "gender": "MALE",
  "dateOfBirth": "1990-05-15",
  "email": "john.doe@example.com",
  "phone": "+91-9876543210",
  "addressLine1": "123 Main Street",
  "city": "Mumbai",
  "stateOrProvince": "Maharashtra",
  "postalCode": "400001",
  "country": "IN",
  "identityDocType": "PASSPORT",
  "identityDocNumber": "A1234567",
  "profilePhotoFileId": null,
  "createdAt": "2025-01-01T00:00:00Z"
}
```

`identityDocNumber` is included in the response. Access is controlled by the `CORE_PERSON_READ` permission. Sensitive use cases (e.g., masking) are a future concern.

### PersonSummaryResponse (list view)
```json
{
  "id": 101,
  "fullName": "John Michael Doe",
  "email": "john.doe@example.com",
  "phone": "+91-9876543210"
}
```

---

## 13. Authorization

| Operation      | Required Permission  |
|----------------|----------------------|
| Create person  | `CORE_PERSON_CREATE` |
| Read person    | `CORE_PERSON_READ`   |
| List persons   | `CORE_PERSON_READ`   |
| Update person  | `CORE_PERSON_UPDATE` |
| Delete person  | `CORE_PERSON_DELETE` |

---

## 14. Internal Service API

Methods exposed as Spring beans to other Core modules:

```
PersonService.existsById(Long id): boolean
PersonService.findById(Long id): PersonResponse
PersonService.findByIdOrThrow(Long id): PersonResponse
```

Used by:
- `core-user` — validates `personId` before creating a user account
- `core-face` — validates `personId` before creating a face profile
- `core-attendance` — validates `personId` before recording attendance
- Business modules — called to retrieve person details when building domain responses

---

## 15. Integration Points

| Module           | Integration                                                      |
|------------------|------------------------------------------------------------------|
| `core-common`    | `SoftDeletableEntity`, exceptions, response types                |
| `core-user`      | `users.person_id` FK; core-user checks person exists before create|
| `core-face`      | `face_profiles.person_id` FK                                     |
| `core-attendance`| `attendance_events.person_id` FK                                 |
| `core-file`      | `profile_photo_file_id` FK → `files(id)`                        |
| `core-audit`     | Audit events for person create, update, delete                   |

---

## 16. Error Handling

| Scenario                                    | Exception                       | HTTP |
|---------------------------------------------|---------------------------------|------|
| Person not found                            | `PersonNotFoundException`       | 404  |
| Email already exists (non-deleted)          | `ResourceAlreadyExistsException`| 409  |
| Delete person with active user              | `ValidationException`           | 409  |
| Delete person with active face profile      | `ValidationException`           | 409  |
| Invalid date of birth (future date)         | `ValidationException` (via BV)  | 400  |
| Profile photo file ID not found             | `ResourceNotFoundException`     | 404  |

---

## 17. Logging and Audit

| Action         | Audit Code        | Details           |
|----------------|-------------------|-------------------|
| Person created | `PERSON_CREATED`  | person_id         |
| Person updated | `PERSON_UPDATED`  | person_id, fields |
| Person deleted | `PERSON_DELETED`  | person_id         |

---

## 18. Security Considerations

- `identityDocNumber` is stored as plain text in V1. Encryption at rest is a future enhancement.
- PII fields (email, phone, identity doc) must never be logged at any log level.
- Profile photo access is controlled via `core-file` permissions.

---

## 19. Future Extensibility

- Business modules extend person data via FK relationships. Core never changes to accommodate domain fields.
- Encryption of PII fields (identity doc number, phone) can be added transparently by wrapping the field in a JPA converter without changing the service API.
- Soft-delete can be upgraded to hard-delete with archival for GDPR compliance without changing the service interface.

---

## 20. Flyway Migrations

```
V2__create_persons_table.sql
```

Note: `persons` table is created before `users` (V3) because `users.person_id` FK references `persons.id`.

---

## 21. Testing Strategy

| Test Type       | Scope                                                              |
|-----------------|--------------------------------------------------------------------|
| Unit — Service  | Create, find, update, soft-delete, existence checks               |
| Unit — Service  | Duplicate email rejection, delete with active user protection     |
| Repository test | `findByEmail`, `search` by name/email/phone, soft-delete filter   |
| Controller test | All endpoints, HTTP codes, validation responses                    |
| Security tests  | `CORE_PERSON_READ` required; 401 without token                    |
| Integration     | Create person → create user → delete person (should fail)         |

---

## 22. Implementation Roadmap

### Task 1: Entity and migration
- `Person` entity extending `SoftDeletableEntity`
- `Gender` enum, `IdentityDocType` enum
- `PersonRepository`
- Flyway: `V2__create_persons_table.sql`

### Task 2: Service — read operations
- `findById`, `findByIdOrThrow`, `existsById`, `searchPersons` with pagination

### Task 3: Service — write operations
- `createPerson`, `updatePerson`, `deletePerson`
- Duplicate email check
- Delete guard (active user / face profile check)

### Task 4: Controller and DTOs
- `PersonController` with all endpoints
- `PersonMapper` (MapStruct) — `fullName` computed field
- Request/response DTOs

### Task 5: Audit integration
- Write audit events for all write operations

---

## 23. Acceptance Criteria

- [ ] Creating a person with a duplicate email returns 409
- [ ] Soft-deleting a person with an active user returns 409
- [ ] `identityDocNumber` is never logged
- [ ] Search by partial name returns matching results
- [ ] `existsById` returns false for soft-deleted persons
- [ ] All write operations produce audit log entries
- [ ] `fullName` in responses is assembled as `firstName [middleName] lastName`

---

## 24. Out of Scope

- Domain-specific identity (roll number, employee ID — belongs in business modules)
- Face profile management (core-face)
- User account creation (core-user)
- GDPR right-to-erasure hard delete
- PII field encryption
