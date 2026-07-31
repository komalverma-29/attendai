# Specification: core-common

## 1. Overview

`core-common` is the shared foundation for the entire `attendai-core` module. It provides no business functionality on its own. Instead, it supplies the building blocks that every other Core sub-module depends on: base entity classes, the API response envelope, a global exception hierarchy, standard error handling, utility classes, constants, and shared configuration infrastructure.

Every Core sub-module depends on `core-common`. Business modules (School, College, Enterprise) inherit these contracts transitively through `attendai-core`.

---

## 2. Scope and Objectives

**In scope:**
- Base JPA entity with audit fields
- Soft-deletable entity variant
- API response envelope (success, paginated success, error)
- Standard exception hierarchy
- Global exception handler (`@RestControllerAdvice`)
- Validation utility helpers
- Pagination request/response models
- Common constants
- String, date, and collection utility classes
- Spring Security utility (current user context)
- `@ConfigurationProperties` base infrastructure

**Out of scope:**
- Any business logic
- Any domain-specific entities
- Authentication or authorization implementation (belongs in `core-auth`)
- Audit log persistence (belongs in `core-audit`)

---

## 3. Functional Requirements

### FR-CM-01: Base Entity
Every JPA entity in the system must extend a base class that provides:
- Surrogate primary key (`id`, `BIGINT UNSIGNED`, auto-increment)
- Audit timestamps: `createdAt`, `updatedAt` (populated automatically by Spring Data JPA auditing)
- Audit user references: `createdBy`, `updatedBy` (populated automatically via `AuditorAware`)
- No business fields

### FR-CM-02: Soft-Deletable Entity
Entities subject to soft delete must extend a variant of the base entity that adds:
- `isDeleted` (`BOOLEAN`, default `false`)
- `deletedAt` (`DATETIME`, nullable)
- A JPA `@Where` filter that automatically excludes soft-deleted records from all queries

### FR-CM-03: API Response Envelope
All API responses must be wrapped in a consistent envelope structure. Three variants are required:
- **Success response**: wraps a single data object
- **Paginated response**: wraps a list with pagination metadata
- **Error response**: wraps error details without exposing internals

### FR-CM-04: Standard Exception Hierarchy
Provide a set of base exception classes that all Core and business modules extend:
- `AttendAIException` — root exception
- `ResourceNotFoundException` — entity lookup failure (HTTP 404)
- `ResourceAlreadyExistsException` — duplicate resource (HTTP 409)
- `ValidationException` — business rule validation failure (HTTP 400)
- `UnauthorizedException` — authentication failure (HTTP 401)
- `ForbiddenException` — authorization failure (HTTP 403)
- `ExternalServiceException` — downstream/integration failure (HTTP 502)

### FR-CM-05: Global Exception Handler
A `@RestControllerAdvice` must handle all standard exceptions and produce error responses conforming to the envelope structure. It must catch:
- Custom `AttendAIException` subtypes
- `MethodArgumentNotValidException` (Bean Validation failures)
- `ConstraintViolationException`
- `HttpMessageNotReadableException` (malformed JSON)
- `NoHandlerFoundException` (404 for unknown routes)
- `AccessDeniedException` (Spring Security 403)
- `AuthenticationException` (Spring Security 401)
- Unhandled `Exception` (500 fallback)

### FR-CM-06: Pagination Models
Standard request and response models for all paginated endpoints:
- `PageRequest` — `page` (0-indexed), `size` (default 20, max 100), `sortBy`, `sortDirection`
- `PageResponse<T>` — `data` list, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`

### FR-CM-07: Security Context Utility
A utility class that retrieves the currently authenticated user's ID and authorities from the Spring Security context. Used by services to determine the acting user without coupling to the HTTP layer.

### FR-CM-08: AuditorAware Implementation
An implementation of Spring Data JPA's `AuditorAware<Long>` that resolves the current user's ID from the Security context. Returns `Optional.empty()` for unauthenticated requests (e.g., during system startup or scheduled jobs).

---

## 4. Non-Functional Requirements

- `core-common` must have zero dependencies on other `attendai-core` sub-modules.
- `core-common` must have no dependency on any business module.
- All classes in `core-common` must be unit-testable without a Spring context where possible.
- The exception hierarchy must be stable. Adding new exception types is permitted; removing or renaming existing ones requires a deprecation cycle.
- Response envelope structure must remain backward-compatible. Field names must not change once established.

---

## 5. Domain Model

`core-common` defines no domain entities — it defines infrastructure contracts that domain entities build upon.

### BaseEntity (abstract, mapped superclass)

| Field       | Type            | Description                              |
|-------------|-----------------|------------------------------------------|
| id          | Long            | Surrogate PK, auto-increment             |
| createdAt   | LocalDateTime   | Set on insert by JPA auditing            |
| updatedAt   | LocalDateTime   | Updated on every save by JPA auditing    |
| createdBy   | Long            | User ID of creator, set by AuditorAware  |
| updatedBy   | Long            | User ID of last modifier                 |

### SoftDeletableEntity (abstract, extends BaseEntity)

| Field       | Type            | Description                              |
|-------------|-----------------|------------------------------------------|
| isDeleted   | boolean         | Soft delete flag, default false          |
| deletedAt   | LocalDateTime   | Timestamp of soft deletion, nullable     |

---

## 6. Data Model and Persistence

`core-common` does not own any database tables. It provides Java-level mapped superclasses only (`@MappedSuperclass`).

The `@EnableJpaAuditing` annotation must be placed on the Spring Boot main application class or a dedicated `JpaConfig` in `core-common`.

`AuditorAware<Long>` bean must be registered as a Spring bean.

---

## 7. Package Organization

```
com.attendai.core.common
├── entity
│   ├── BaseEntity.java
│   └── SoftDeletableEntity.java
├── response
│   ├── ApiResponse.java
│   ├── PageResponse.java
│   └── ErrorResponse.java
├── exception
│   ├── AttendAIException.java
│   ├── ResourceNotFoundException.java
│   ├── ResourceAlreadyExistsException.java
│   ├── ValidationException.java
│   ├── UnauthorizedException.java
│   ├── ForbiddenException.java
│   └── ExternalServiceException.java
├── handler
│   └── GlobalExceptionHandler.java
├── pagination
│   ├── PageRequestParams.java
│   └── PageMetadata.java
├── security
│   └── SecurityContextUtils.java
├── audit
│   └── AttendAIAuditorAware.java
├── config
│   └── JpaAuditingConfig.java
├── util
│   ├── DateUtils.java
│   ├── StringUtils.java
│   └── CollectionUtils.java
└── constants
    └── AttendAIConstants.java
```

---

## 8. API Response Envelope

### Success Response
```json
{
  "success": true,
  "data": { }
}
```

### Paginated Success Response
```json
{
  "success": true,
  "data": [ ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource with id 42 was not found",
    "timestamp": "2025-01-15T10:30:00Z",
    "path": "/api/v1/core/users/42"
  }
}
```

### Validation Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "timestamp": "2025-01-15T10:30:00Z",
    "path": "/api/v1/core/users",
    "fieldErrors": [
      { "field": "email", "message": "must be a valid email address" },
      { "field": "firstName", "message": "must not be blank" }
    ]
  }
}
```

---

## 9. Exception Hierarchy and HTTP Mapping

| Exception Class                  | HTTP Status | Error Code              |
|----------------------------------|-------------|-------------------------|
| `ResourceNotFoundException`      | 404         | `NOT_FOUND`             |
| `ResourceAlreadyExistsException` | 409         | `ALREADY_EXISTS`        |
| `ValidationException`            | 400         | `VALIDATION_FAILED`     |
| `UnauthorizedException`          | 401         | `UNAUTHORIZED`          |
| `ForbiddenException`             | 403         | `FORBIDDEN`             |
| `ExternalServiceException`       | 502         | `EXTERNAL_SERVICE_ERROR`|
| `MethodArgumentNotValidException`| 400         | `VALIDATION_FAILED`     |
| `ConstraintViolationException`   | 400         | `VALIDATION_FAILED`     |
| `HttpMessageNotReadableException`| 400         | `MALFORMED_REQUEST`     |
| `AccessDeniedException`          | 403         | `FORBIDDEN`             |
| `AuthenticationException`        | 401         | `UNAUTHORIZED`          |
| `Exception` (fallback)           | 500         | `INTERNAL_ERROR`        |

**Security rule:** The 500 fallback handler must log the full exception internally but return only a generic message to the client. Stack traces must never appear in API responses.

---

## 10. Constants

`AttendAIConstants` must define:
- Default page size: `20`
- Maximum page size: `100`
- Default sort direction: `ASC`
- Date/time format patterns for logging
- Common regex patterns (email, phone)

---

## 11. Configuration

`JpaAuditingConfig` must:
- Be annotated with `@Configuration` and `@EnableJpaAuditing`
- Register the `AttendAIAuditorAware` bean
- Not contain any other Spring configuration

---

## 12. Security Considerations

- `SecurityContextUtils` must never throw an exception when no authentication is present. It must return `Optional.empty()`.
- Error responses must never include stack traces, SQL details, or class names in any profile.
- The `GlobalExceptionHandler` must sanitize all 500 responses.

---

## 13. Logging Requirements

- The `GlobalExceptionHandler` logs at `WARN` level for 4xx errors and `ERROR` level for 5xx errors.
- Log format for exceptions: `[HTTP <status>] <ErrorCode> - <message> | path=<path>`
- Never log request body content in exception handlers (may contain PII or credentials).

---

## 14. Integration Points

`core-common` is consumed by all other Core modules. It has no outbound integrations. It does not call any other module.

Modules that depend on `core-common`:
- `core-auth`
- `core-user`
- `core-role`
- `core-permission`
- `core-person`
- `core-face`
- `core-attendance`
- `core-station`
- `core-notification`
- `core-file`
- `core-audit`
- `core-config`

---

## 15. Testing Strategy

| Test Type       | Scope                                                                 |
|-----------------|-----------------------------------------------------------------------|
| Unit tests      | `GlobalExceptionHandler`, `SecurityContextUtils`, utility classes     |
| Unit tests      | Each exception class construction and message formatting              |
| Unit tests      | `ApiResponse`, `PageResponse` builder/factory correctness             |
| Unit tests      | `AttendAIAuditorAware` with mocked security context                   |
| No DB tests     | `core-common` has no database tables                                  |

Test class naming: `GlobalExceptionHandlerTest`, `SecurityContextUtilsTest`, `ApiResponseTest`.

All tests use JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`.

---

## 16. Implementation Roadmap

### Task 1: Base entity and soft-delete entity
- Create `BaseEntity` with `@MappedSuperclass`, `@EntityListeners`, audit fields
- Create `SoftDeletableEntity` extending `BaseEntity` with `isDeleted`, `deletedAt`
- Create `JpaAuditingConfig` with `@EnableJpaAuditing`
- Create `AttendAIAuditorAware` bean

### Task 2: Exception hierarchy
- Create `AttendAIException` root with `errorCode` and `message` fields
- Create all six concrete exception subclasses
- Write unit tests for each

### Task 3: API response models
- Create `ApiResponse<T>` with factory methods `success(T data)` and `error(ErrorResponse error)`
- Create `PageResponse<T>` with pagination metadata
- Create `ErrorResponse` with `fieldErrors` support
- Write unit tests

### Task 4: Global exception handler
- Implement `GlobalExceptionHandler` with `@RestControllerAdvice`
- Handle all exception types listed in section 9
- Write unit tests covering each handler method

### Task 5: Pagination models
- Create `PageRequestParams` (query param parsing)
- Create `PageMetadata` (response metadata)

### Task 6: Security context utility
- Implement `SecurityContextUtils` with `getCurrentUserId()` and `getCurrentUserAuthorities()`
- Write unit tests with mocked `SecurityContext`

### Task 7: Utility classes and constants
- Implement `DateUtils`, `StringUtils`, `CollectionUtils`
- Define `AttendAIConstants`

---

## 17. Acceptance Criteria

- [ ] All Core sub-module entities extend `BaseEntity` or `SoftDeletableEntity`
- [ ] `created_at`, `updated_at`, `created_by`, `updated_by` are populated automatically on every entity save
- [ ] Soft-deleted entities are never returned by standard JPA queries
- [ ] All API success responses conform to the envelope structure
- [ ] All API error responses conform to the error envelope structure
- [ ] No stack trace is present in any API error response
- [ ] `MethodArgumentNotValidException` produces a `fieldErrors` array in the response
- [ ] `GlobalExceptionHandler` logs at WARN for 4xx and ERROR for 5xx
- [ ] `SecurityContextUtils.getCurrentUserId()` returns `Optional.empty()` when unauthenticated
- [ ] All unit tests pass

---

## 18. Out of Scope

- Audit log persistence (core-audit)
- Authentication or JWT handling (core-auth)
- Any domain-specific entity or concept
- Database migration scripts (core-common has no tables)
- Internationalization / localization of error messages
