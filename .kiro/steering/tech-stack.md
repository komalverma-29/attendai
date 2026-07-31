# AttendAI — Technology Stack

## Language & Runtime

| Component | Choice      | Notes                          |
|-----------|-------------|--------------------------------|
| Language  | Java 21     | LTS release, virtual threads   |
| Runtime   | JVM         | Standard JVM deployment        |

Java 21 features to leverage:
- Records (for immutable DTOs where appropriate)
- Sealed classes (for domain result types)
- Pattern matching for `instanceof`
- Text blocks (for SQL in tests, JSON templates)
- Virtual threads (for high-throughput I/O)

---

## Framework

| Component      | Choice           | Version    |
|----------------|------------------|------------|
| Framework      | Spring Boot      | 3.x (latest stable) |
| Web            | Spring Web MVC   | Included in Boot    |
| Security       | Spring Security  | Included in Boot    |
| Data Access    | Spring Data JPA  | Included in Boot    |
| ORM            | Hibernate        | Included via JPA    |

Spring Boot 3.x requires Java 17 minimum. Java 21 is the chosen runtime.

---

## Database

| Component         | Choice   | Notes                              |
|-------------------|----------|------------------------------------|
| Database          | MariaDB  | Production database                |
| Schema versioning | Flyway   | Mandatory — no auto DDL            |
| Connection pool   | HikariCP | Default in Spring Boot             |
| Test database     | H2       | In-memory for unit/integration tests |

`spring.jpa.hibernate.ddl-auto` must be set to `validate` in all non-test profiles.
Flyway manages all schema changes. No exceptions.

---

## Build & Dependency Management

| Component         | Choice                |
|-------------------|-----------------------|
| Build tool        | Maven                 |
| Project structure | Maven Multi-Module    |
| Java version mgmt | Defined in parent POM |

Parent POM responsibilities:
- Define `<java.version>21</java.version>`
- Manage all dependency versions via `<dependencyManagement>`
- Manage plugin versions via `<pluginManagement>`
- No business logic in parent POM

---

## Core Libraries

| Library           | Purpose                                    | Version Source         |
|-------------------|--------------------------------------------|------------------------|
| Lombok            | Boilerplate reduction (`@Getter`, `@Builder`, etc.) | Spring Boot BOM |
| MapStruct         | Compile-time DTO ↔ entity mapping          | Explicit version       |
| JWT (jjwt)        | JWT creation, signing, and validation      | `io.jsonwebtoken:jjwt` |
| Jakarta Validation| Bean Validation (was `javax.validation`)   | Included in Boot       |
| SLF4J + Logback   | Logging                                    | Included in Boot       |
| Spring Actuator   | Health checks, metrics endpoints           | Included in Boot       |

---

## Testing Libraries

| Library          | Purpose                                       |
|------------------|-----------------------------------------------|
| JUnit 5          | Test runner and assertions                    |
| Mockito          | Mocking in unit tests                         |
| Spring Boot Test | Integration test support (`@SpringBootTest`)  |
| MockMvc          | Controller layer testing without full server  |
| Testcontainers   | Integration tests with real MariaDB instance  |
| H2               | Fast in-memory database for unit tests        |
| AssertJ          | Fluent assertions (included via Boot Test)    |

---

## AI / Face Recognition

The face recognition engine integration is handled in `core-face`. The specific AI library or API will be determined during `core-face` implementation. The architecture isolates face recognition behind a service interface so the underlying library can be swapped without affecting other modules.

---

## Annotation Processing Order (Maven)

Lombok and MapStruct both use annotation processing. The correct compiler plugin configuration is required:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
    </path>
</annotationProcessorPaths>
```

Lombok must be listed before MapStruct so that MapStruct can see Lombok-generated methods during compilation.

---

## application.yml Structure

Use `application.yml` (not `.properties`) for all configuration. Structure:

```
application.yml              ← Base configuration
application-dev.yml          ← Development overrides
application-test.yml         ← Test overrides
application-prod.yml         ← Production overrides
```

Active profile is set via `SPRING_PROFILES_ACTIVE` environment variable.

---

## Common Commands

```bash
# Build all modules
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run tests for a specific module
mvn test -pl attendai-core

# Run school module
mvn spring-boot:run -pl attendai-school

# Flyway info
mvn flyway:info -pl attendai-school

# Flyway migrate
mvn flyway:migrate -pl attendai-school
```

---

## Dependency Version Policy

- All third-party dependency versions are declared in the parent POM `<dependencyManagement>` section.
- Child modules declare dependencies without versions.
- Versions are never duplicated across modules.
- Use Spring Boot BOM for all Spring-managed libraries.
- Pin non-BOM library versions explicitly; do not use open ranges.
