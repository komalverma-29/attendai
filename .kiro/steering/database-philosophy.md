# AttendAI — Database Philosophy

## Core Principle

The database schema is the source of truth. It is versioned, deterministic, and never modified outside of a migration script.

---

## Schema Management

- **Flyway** manages all schema changes. This is mandatory and has no exceptions.
- `spring.jpa.hibernate.ddl-auto` must be `validate` in `dev` and `prod` profiles.
- `spring.jpa.hibernate.ddl-auto` may be `create-drop` or `none` in the `test` profile with H2.
- Flyway migrations run automatically on application startup.
- Migration scripts are never edited after they have been committed and applied.
- Broken migrations must be fixed with a new migration script, not by editing the old one.

### Migration File Naming

```
V<version>__<description>.sql
```

Examples:
```
V1__create_users_table.sql
V2__create_roles_table.sql
V3__add_user_role_mapping.sql
V10__create_school_table.sql
V11__create_students_table.sql
V12__add_student_roll_number_index.sql
```

Rules:
- Version numbers are sequential integers. No decimals.
- Description uses lowercase words separated by underscores.
- One logical concern per migration file. Do not bundle unrelated changes.
- Migration files live in `src/main/resources/db/migration/` within each module.

---

## Naming Conventions

### Tables

- Use `snake_case`.
- Use plural names for entity tables.
- Prefix with module context when needed to avoid collisions.

| Module  | Pattern              | Example                        |
|---------|----------------------|--------------------------------|
| Core    | No prefix            | `users`, `roles`, `permissions`|
| School  | `school_` prefix     | `school_students`, `school_classes` |

### Columns

- Use `snake_case` for all column names.
- Boolean columns use `is_` prefix: `is_active`, `is_deleted`, `is_verified`.
- Foreign key columns follow `<referenced_table_singular>_id` pattern: `user_id`, `school_id`, `student_id`.
- Avoid abbreviations. Prefer `date_of_birth` over `dob`.

### Primary Keys

- All tables use a surrogate primary key named `id`.
- Type: `BIGINT UNSIGNED AUTO_INCREMENT` for MariaDB.
- Never use domain values (roll number, email) as primary keys.

### Indexes

- Index naming: `idx_<table>_<column(s)>`
- Unique constraint naming: `uq_<table>_<column(s)>`
- Foreign key naming: `fk_<table>_<referenced_table>`

Examples:
```sql
INDEX idx_users_email (email),
UNIQUE uq_users_email (email),
CONSTRAINT fk_users_roles FOREIGN KEY (role_id) REFERENCES roles(id)
```

---

## Audit Fields

Every entity table that represents a domain object must include audit fields:

```sql
created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
created_by   BIGINT UNSIGNED,
updated_by   BIGINT UNSIGNED
```

These map to a `BaseEntity` or `Auditable` abstract class in Java, using Spring Data JPA auditing (`@EntityListeners(AuditingEntityListener.class)`) with `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`.

Enable JPA auditing in the application with `@EnableJpaAuditing`.

---

## Soft Delete Policy

- Entities that should not be hard-deleted use a soft delete flag.
- Soft delete column: `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`.
- Add a `deleted_at DATETIME NULL` column alongside it.
- Spring Data JPA `@Where(clause = "is_deleted = false")` filters deleted records automatically.
- Hard delete is only permitted for lookup/reference data or during test setup.
- Audit logs are never soft-deleted.

Entities that use soft delete in Core:
- `users`
- `persons`
- `face_profiles`

Entities that use soft delete in School:
- `school_students`
- `school_teachers`
- `school_schools`

---

## Referential Integrity

- Foreign key constraints must be defined in the database, not only in the ORM.
- `ON DELETE RESTRICT` is the default behaviour. Explicit cascades require justification.
- Orphan records are not permitted. Relationships are enforced at the DB level.
- Join tables for many-to-many relationships use composite primary keys.

---

## Indexing Strategy

Apply indexes to:
- All foreign key columns (MariaDB does not create these automatically).
- All columns used in `WHERE` clauses in common queries.
- All columns used in `ORDER BY` for paginated queries.
- Unique constraints on natural identifiers (email, roll number, username).

Do not over-index. Index write-heavy tables conservatively. Review `EXPLAIN` output for complex queries.

---

## Timestamp Conventions

- Store all timestamps in UTC in the database.
- Column type: `DATETIME` (not `TIMESTAMP` — avoids year 2038 and timezone shift issues).
- Java type: `LocalDateTime` for `created_at`/`updated_at`, `LocalDate` for date-only fields (e.g., `date_of_birth`, `holiday_date`).
- Never store timestamps as epoch integers.
- MariaDB session timezone must be set to UTC.

---

## Data Type Guidelines

| Data                        | MariaDB Type                  | Java Type          |
|-----------------------------|-------------------------------|--------------------|
| Primary key                 | `BIGINT UNSIGNED`             | `Long`             |
| Short text (name, code)     | `VARCHAR(255)`                | `String`           |
| Long text (description)     | `TEXT`                        | `String`           |
| Boolean flag                | `BOOLEAN` (`TINYINT(1)`)      | `boolean`          |
| Date only                   | `DATE`                        | `LocalDate`        |
| Date and time               | `DATETIME`                    | `LocalDateTime`    |
| Decimal (money, percentage) | `DECIMAL(10,2)`               | `BigDecimal`       |
| Enum/status                 | `VARCHAR(50)`                 | Java `enum`        |
| JSON data                   | `JSON`                        | `String` or typed  |
| File path / URL             | `VARCHAR(500)`                | `String`           |

Avoid `ENUM` type in MariaDB. Store enum values as `VARCHAR` and validate in the application layer.

---

## No Auto Schema Generation

`ddl-auto: validate` means Hibernate will:
1. Read the current database schema.
2. Compare it against entity mappings.
3. Throw an error if they do not match.

This catches drift between migrations and entities at startup, not at runtime. All schema changes must have a corresponding Flyway migration.

---

## Test Database

- Unit and repository tests use H2 in-memory database.
- Integration tests that require MariaDB-specific behaviour use Testcontainers.
- H2 test migrations live in `src/test/resources/db/migration/` or use `spring.flyway.locations`.
- Test data is set up programmatically or via `@Sql` annotations. No shared persistent test data.
