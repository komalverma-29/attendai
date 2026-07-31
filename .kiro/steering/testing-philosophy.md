# AttendAI — Testing Philosophy

## Guiding Principle

Tests are not optional. Every feature ships with tests. Tests define expected behaviour, catch regressions, and document intent. A class without tests is considered incomplete.

---

## Test Types

### 1. Unit Tests

Test a single class in isolation. All dependencies are mocked.

- Scope: Service layer, domain logic, mappers, utility classes.
- Framework: JUnit 5 + Mockito.
- Database: None. No Spring context loaded.
- Speed: Fast (milliseconds per test).
- Naming: `<ClassName>Test`

Use `@ExtendWith(MockitoExtension.class)` for pure Mockito tests without Spring.

```java
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void createStudent_shouldReturnStudentResponse_whenValidRequest() {
        // Arrange
        // Act
        // Assert
    }
}
```

---

### 2. Repository Tests

Test JPA repositories against an actual database using H2 in-memory.

- Scope: Spring Data JPA repositories, custom JPQL queries.
- Framework: JUnit 5 + `@DataJpaTest`.
- Database: H2 in-memory with Flyway migrations.
- Speed: Medium (seconds per test class due to context startup).
- Naming: `<ClassName>RepositoryTest`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void findByRollNumber_shouldReturnStudent_whenExists() {
        // Arrange + Act + Assert
    }
}
```

---

### 3. Controller Tests (Slice Tests)

Test the controller layer in isolation using MockMvc. No full application context.

- Scope: REST endpoints, request validation, response structure, HTTP status codes.
- Framework: JUnit 5 + `@WebMvcTest` + MockMvc + Mockito.
- Database: None. Service layer is mocked.
- Speed: Medium.
- Naming: `<ClassName>ControllerTest`

```java
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Test
    void createStudent_shouldReturn201_whenValidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/school/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"firstName\": \"John\", ... }"))
            .andExpect(status().isCreated());
    }
}
```

---

### 4. Integration Tests

Test the full application stack end-to-end including the database.

- Scope: Full request-response cycle, cross-layer integration, security filters, real SQL.
- Framework: JUnit 5 + `@SpringBootTest` + MockMvc or RestAssured.
- Database: Testcontainers (real MariaDB) or H2 depending on test needs.
- Speed: Slow (seconds to start). Run separately from unit tests.
- Naming: `<Feature>IntegrationTest`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StudentEnrollmentIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.11");

    @Test
    void enrollStudent_shouldPersistAndReturnStudent() {
        // Full stack test
    }
}
```

---

### 5. Security Tests

Test that authorization rules are enforced correctly.

- Scope: Endpoint access control, role-permission enforcement, unauthenticated access rejection.
- Framework: `@WebMvcTest` + Spring Security Test (`@WithMockUser`, `SecurityMockMvcRequestPostProcessors`).
- Must verify both positive (access granted) and negative (access denied) cases.

```java
@Test
@WithMockUser(authorities = "SCHOOL_STUDENT_READ")
void getStudent_shouldReturn200_whenAuthorized() throws Exception { ... }

@Test
void getStudent_shouldReturn401_whenNotAuthenticated() throws Exception { ... }

@Test
@WithMockUser(authorities = "SCHOOL_TEACHER_READ")
void getStudent_shouldReturn403_whenWrongPermission() throws Exception { ... }
```

---

## Test Naming Convention

All test method names follow the pattern:

```
<methodUnderTest>_should<ExpectedBehaviour>_when<Condition>
```

Examples:
- `createStudent_shouldReturnStudentResponse_whenValidRequest`
- `createStudent_shouldThrowAlreadyExistsException_whenRollNumberIsDuplicate`
- `findStudentById_shouldThrowNotFoundException_whenStudentDoesNotExist`
- `deleteStudent_shouldSoftDelete_whenStudentExists`
- `getStudent_shouldReturn401_whenTokenIsMissing`

This naming style makes test failure messages immediately readable without opening the test file.

---

## Test Structure — AAA Pattern

All tests follow the Arrange-Act-Assert structure, with clear section separation:

```java
@Test
void createStudent_shouldReturnStudentResponse_whenValidRequest() {
    // Arrange
    CreateStudentRequest request = new CreateStudentRequest(...);
    Student student = new Student(...);
    StudentResponse expected = new StudentResponse(...);
    when(studentRepository.save(any())).thenReturn(student);
    when(studentMapper.toResponse(student)).thenReturn(expected);

    // Act
    StudentResponse result = studentService.createStudent(request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getRollNumber()).isEqualTo(expected.getRollNumber());
    verify(studentRepository).save(any(Student.class));
}
```

---

## Test Coverage Expectations

| Layer       | Expected Coverage |
|-------------|-------------------|
| Service     | ≥ 80%             |
| Repository  | ≥ 70% (key queries)|
| Controller  | ≥ 70%             |
| Mapper      | ≥ 60%             |

Coverage is a floor, not a target. A meaningless test that only inflates coverage is worse than no test.

Priority order for coverage:
1. Business logic (service layer)
2. Edge cases and error paths
3. Authorization rules
4. Happy paths

---

## Test Data

- Test data is created inline within each test. No shared mutable global state.
- Use builder patterns or factory methods for constructing test objects.
- Never rely on database state left over from a previous test. Each test is independent.
- For integration tests, use `@Transactional` on the test class to roll back after each test, or truncate tables in a `@BeforeEach` setup.
- Never use production data in tests.

---

## Test Organization

- Test files mirror the production package structure.
- Unit tests live in `src/test/java/<same-package-as-production-class>/`.
- Integration tests may live in a dedicated `integration` sub-package.
- Test utility classes and factories live in `src/test/java/com/attendai/<module>/support/`.

---

## Test Profiles

- Tests run with the `test` Spring profile active.
- `application-test.yml` configures H2, disables Flyway auto-migration where needed, and uses test-safe defaults.
- The `test` profile must never connect to a shared or production database.

---

## What Not to Test

- Do not test Lombok-generated methods.
- Do not test MapStruct-generated mapper implementations directly (test via service).
- Do not test Spring Boot auto-configuration.
- Do not test framework behaviour (Spring, Hibernate).
- Focus tests on code that the team writes and owns.
