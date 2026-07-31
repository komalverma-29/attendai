# AttendAI — Coding Standards

## General Principles

- Follow SOLID principles in all code.
- Favour composition over inheritance.
- Prefer explicit over implicit.
- Write code that is readable first, clever never.
- Every class, method, and field must have a clear single responsibility.
- Avoid premature abstraction. Add layers only when they are needed.

---

## Package Naming

All packages use lowercase, dot-separated names.

### Core module packages
```
com.attendai.core.<module>.<layer>
```
Example:
```
com.attendai.core.auth.controller
com.attendai.core.auth.service
com.attendai.core.auth.repository
com.attendai.core.auth.entity
com.attendai.core.auth.dto
com.attendai.core.auth.mapper
com.attendai.core.auth.exception
com.attendai.core.auth.config
```

### School module packages
```
com.attendai.school.<module>.<layer>
```
Example:
```
com.attendai.school.student.controller
com.attendai.school.student.service
com.attendai.school.student.repository
com.attendai.school.student.entity
com.attendai.school.student.dto
com.attendai.school.student.mapper
com.attendai.school.student.exception
```

---

## Class Naming

| Type                    | Convention                               | Example                        |
|-------------------------|------------------------------------------|--------------------------------|
| JPA Entity              | `<Domain>`                               | `Student`, `AttendanceEvent`   |
| Spring Service          | `<Domain>Service`                        | `StudentService`               |
| Service implementation  | `<Domain>ServiceImpl`                    | `StudentServiceImpl`           |
| Spring Repository       | `<Domain>Repository`                     | `StudentRepository`            |
| REST Controller         | `<Domain>Controller`                     | `StudentController`            |
| Request DTO             | `<Domain>Request` or `Create<Domain>Request` | `CreateStudentRequest`     |
| Response DTO            | `<Domain>Response`                       | `StudentResponse`              |
| Update DTO              | `Update<Domain>Request`                  | `UpdateStudentRequest`         |
| MapStruct Mapper        | `<Domain>Mapper`                         | `StudentMapper`                |
| Exception               | `<Domain>NotFoundException`              | `StudentNotFoundException`     |
| Configuration class     | `<Domain>Config`                         | `SecurityConfig`, `JwtConfig`  |
| Constants class         | `<Domain>Constants`                      | `AttendanceConstants`          |
| Enum                    | `<Domain>Status`, `<Domain>Type`         | `AttendanceStatus`, `LeaveType`|

---

## Method Naming

| Operation        | Convention             | Example                          |
|------------------|------------------------|----------------------------------|
| Create           | `create`               | `createStudent(...)`             |
| Read single      | `find` or `get`        | `findStudentById(...)`, `getById`|
| Read list        | `findAll`, `list`      | `findAllStudents(...)`           |
| Update           | `update`               | `updateStudent(...)`             |
| Delete           | `delete`               | `deleteStudent(...)`             |
| Existence check  | `exists`               | `existsByRollNumber(...)`        |
| Validation       | `validate`             | `validateEnrollment(...)`        |
| Convert/Map      | `to<Type>`             | `toResponse(...)`, `toEntity(...)` |

Service methods that retrieve an entity and throw if not found must use `findById(id)` naming and throw the module-specific `NotFoundException`.

---

## DTO Standards

- Request DTOs carry incoming data. They must have Bean Validation annotations.
- Response DTOs carry outgoing data. They must never contain sensitive fields (passwords, tokens).
- DTOs must never contain JPA annotations (`@Entity`, `@Column`, `@OneToMany`, etc.).
- DTOs should be immutable where practical. Use Lombok `@Value` or Java records.
- Separate Request and Response DTOs. Never reuse a single DTO for both.

### Validation on Request DTOs

```java
public class CreateStudentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Roll number is required")
    private String rollNumber;
}
```

---

## Service Layer Standards

- Services contain all business logic.
- Services must be interfaces with a single implementation class.
- Service implementations are annotated with `@Service`.
- Transactional boundaries are declared on service methods, not controllers.
- Use `@Transactional` for write operations and `@Transactional(readOnly = true)` for reads.
- Services must not return JPA entities directly. They map to DTOs before returning.

```java
public interface StudentService {
    StudentResponse createStudent(CreateStudentRequest request);
    StudentResponse findStudentById(Long id);
    Page<StudentResponse> findAllStudents(Pageable pageable);
    StudentResponse updateStudent(Long id, UpdateStudentRequest request);
    void deleteStudent(Long id);
}
```

---

## Repository Layer Standards

- All repositories extend `JpaRepository<Entity, ID>`.
- Custom queries use JPQL (`@Query`) or Spring Data method name conventions.
- Native SQL queries are permitted only when JPQL is insufficient; must be documented.
- No business logic in repositories.
- Repository methods that check existence return `boolean` or `Optional`.

---

## Controller Layer Standards

- Controllers handle HTTP only: path mapping, request parsing, response building.
- No business logic in controllers.
- All controllers are annotated with `@RestController` and `@RequestMapping`.
- Controllers delegate immediately to the service layer.
- Input validation is triggered via `@Valid` on method parameters.
- Controllers return `ResponseEntity<T>` with explicit HTTP status codes.

```java
@RestController
@RequestMapping("/api/v1/school/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.createStudent(request));
    }
}
```

---

## Exception Handling

- Each module defines its own exception hierarchy.
- Base exceptions extend `RuntimeException`.
- Common exception types: `NotFoundException`, `AlreadyExistsException`, `ValidationException`, `UnauthorizedException`.
- A `@RestControllerAdvice` in each module (or in `core-common`) handles exceptions and returns standard error responses.
- Never return stack traces to clients in production.
- HTTP status codes:
  - `400 Bad Request` — validation failures, malformed input
  - `401 Unauthorized` — authentication failure
  - `403 Forbidden` — authorization failure
  - `404 Not Found` — entity not found
  - `409 Conflict` — duplicate resource
  - `500 Internal Server Error` — unexpected failures

### Standard Error Response Body

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Student with id 42 was not found",
  "timestamp": "2025-01-15T10:30:00Z",
  "path": "/api/v1/school/students/42"
}
```

---

## Dependency Injection

- Use **constructor injection** exclusively.
- Never use field injection (`@Autowired` on fields).
- Use Lombok `@RequiredArgsConstructor` to generate constructors.
- Optional dependencies use `@Autowired` setter injection only if truly optional.

```java
// Correct
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
}

// Forbidden
@Service
public class StudentServiceImpl {
    @Autowired
    private StudentRepository studentRepository; // ← Never do this
}
```

---

## Lombok Usage

Permitted annotations:
- `@Getter`, `@Setter` — on entity fields selectively, or on class
- `@Builder` — for builder pattern on entities and DTOs
- `@RequiredArgsConstructor` — for constructor injection
- `@NoArgsConstructor`, `@AllArgsConstructor` — for JPA/DTO needs
- `@ToString` — use with `exclude` for relationships to avoid circular printing
- `@EqualsAndHashCode` — use with `onlyExplicitlyIncluded` on entities

Avoid:
- `@Data` on JPA entities (generates problematic `equals`/`hashCode` on all fields)
- `@SneakyThrows` (hides checked exceptions)

---

## MapStruct Usage

- One mapper interface per entity.
- Annotate with `@Mapper(componentModel = "spring")` so Spring manages the bean.
- Use `@Mapping` for field-name differences.
- Mappers must not contain business logic.

```java
@Mapper(componentModel = "spring")
public interface StudentMapper {
    StudentResponse toResponse(Student student);
    Student toEntity(CreateStudentRequest request);
}
```

---

## Logging Standards

- Use SLF4J `Logger` via Lombok `@Slf4j`.
- Log at appropriate levels:
  - `ERROR` — unexpected failures requiring immediate attention
  - `WARN` — recoverable issues or unexpected but handled state
  - `INFO` — significant business events (entity created, process started/completed)
  - `DEBUG` — internal flow details useful during development
  - `TRACE` — very verbose low-level tracing (almost never in production)
- Never log passwords, tokens, or PII.
- Log at service boundaries, not in repositories or controllers.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("Creating student with roll number: {}", request.getRollNumber());
        // ...
    }
}
```

---

## API Response Envelope

All API responses use a consistent wrapper from `core-common`:

```json
// Success
{
  "success": true,
  "data": { ... }
}

// Paginated success
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}

// Error
{
  "success": false,
  "error": {
    "status": 404,
    "code": "NOT_FOUND",
    "message": "Student not found"
  }
}
```

---

## Code Organization Within a Module

Files within a module sub-package follow this order:

1. Entity classes
2. Repository interfaces
3. Service interfaces
4. Service implementations
5. Controller classes
6. DTO classes (request, response)
7. Mapper interfaces
8. Exception classes
9. Configuration classes (if module-specific)

---

## Constants

- Use `final` classes with a private constructor for constant holders.
- Group constants by domain concept.
- Never use magic strings or magic numbers inline.

```java
public final class AttendanceConstants {
    private AttendanceConstants() {}

    public static final int MAX_LATE_MINUTES = 15;
    public static final String DEFAULT_STATUS = "PRESENT";
}
```
