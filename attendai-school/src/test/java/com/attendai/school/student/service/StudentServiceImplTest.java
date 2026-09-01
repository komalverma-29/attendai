package com.attendai.school.student.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.service.PersonService;
import com.attendai.core.role.service.RoleService;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.service.UserService;
import com.attendai.school.school.service.SchoolService;
import com.attendai.school.student.dto.ChangeStudentStatusRequest;
import com.attendai.school.student.dto.EnrollStudentRequest;
import com.attendai.school.student.entity.SchoolStudent;
import com.attendai.school.student.entity.StudentStatus;
import com.attendai.school.student.exception.StudentNotFoundException;
import com.attendai.school.student.mapper.SchoolStudentMapper;
import com.attendai.school.student.repository.SchoolStudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock SchoolStudentRepository studentRepository;
    @Mock SchoolStudentMapper     studentMapper;
    @Mock SchoolService           schoolService;
    @Mock PersonService           personService;
    @Mock UserService             userService;
    @Mock RoleService             roleService;
    @Mock AuditService            auditService;

    private StudentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentServiceImpl(studentRepository, studentMapper, schoolService,
                personService, userService, roleService, auditService);
    }

    // -------------------------------------------------------------------------
    // enrollStudent
    // -------------------------------------------------------------------------

    @Test
    void enrollStudent_shouldSave_whenValidRequest() {
        // Arrange
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(studentRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(false);
        when(studentRepository.existsBySchoolIdAndAdmissionNumber(1L, "ADM-001")).thenReturn(false);

        SchoolStudent saved = buildStudent(1L, 1L, 10L, null, StudentStatus.ACTIVE);
        when(studentRepository.save(any())).thenReturn(saved);
        when(studentMapper.toResponse(saved)).thenReturn(null);

        // Act
        service.enrollStudent(1L, buildEnrollRequest(10L, "ADM-001"));

        // Assert
        verify(studentRepository).save(any(SchoolStudent.class));
        verify(auditService).log(any());
    }

    @Test
    void enrollStudent_shouldThrow_whenSchoolNotActive() {
        // Arrange
        when(schoolService.isActive(1L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.enrollStudent(1L, buildEnrollRequest(10L, "ADM-001")))
                .isInstanceOf(ValidationException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_shouldThrow409_whenPersonAlreadyEnrolled() {
        // Arrange
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(studentRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.enrollStudent(1L, buildEnrollRequest(10L, "ADM-001")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void enrollStudent_shouldThrow409_whenAdmissionNumberDuplicate() {
        // Arrange
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(studentRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(false);
        when(studentRepository.existsBySchoolIdAndAdmissionNumber(1L, "ADM-001")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.enrollStudent(1L, buildEnrollRequest(10L, "ADM-001")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenStudentBelongsToDifferentSchool() {
        // Arrange — student belongs to school 2, not 1
        SchoolStudent student = buildStudent(1L, 2L, 10L, null, StudentStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        // Act + Assert
        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenStudentNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(StudentNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldUpdateStatus_whenCurrentStatusIsActive() {
        // Arrange
        SchoolStudent student = buildStudent(1L, 1L, 10L, null, StudentStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any())).thenReturn(student);
        when(studentMapper.toResponse(any())).thenReturn(null);

        ChangeStudentStatusRequest req = new ChangeStudentStatusRequest();
        req.setStatus(StudentStatus.INACTIVE);

        // Act
        service.changeStatus(1L, 1L, req);

        // Assert
        assertThat(student.getStatus()).isEqualTo(StudentStatus.INACTIVE);
    }

    @Test
    void changeStatus_shouldThrow_whenStudentIsInTerminalState_transferred() {
        // Arrange — TRANSFERRED is terminal
        SchoolStudent student = buildStudent(1L, 1L, 10L, null, StudentStatus.TRANSFERRED);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        ChangeStudentStatusRequest req = new ChangeStudentStatusRequest();
        req.setStatus(StudentStatus.ACTIVE);

        // Act + Assert
        assertThatThrownBy(() -> service.changeStatus(1L, 1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    void changeStatus_shouldThrow_whenStudentIsInTerminalState_graduated() {
        // Arrange — GRADUATED is terminal
        SchoolStudent student = buildStudent(1L, 1L, 10L, null, StudentStatus.GRADUATED);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        ChangeStudentStatusRequest req = new ChangeStudentStatusRequest();
        req.setStatus(StudentStatus.ACTIVE);

        assertThatThrownBy(() -> service.changeStatus(1L, 1L, req))
                .isInstanceOf(ValidationException.class);
    }

    // -------------------------------------------------------------------------
    // removeUser
    // -------------------------------------------------------------------------

    @Test
    void removeUser_shouldThrow_whenNoUserLinked() {
        SchoolStudent student = buildStudent(1L, 1L, 10L, null, StudentStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.removeUser(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not have a linked user");
    }

    // -------------------------------------------------------------------------
    // deleteStudent
    // -------------------------------------------------------------------------

    @Test
    void deleteStudent_shouldSoftDelete_whenStudentExists() {
        // Arrange
        SchoolStudent student = buildStudent(1L, 1L, 10L, null, StudentStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any())).thenReturn(student);

        // Act
        service.deleteStudent(1L, 1L);

        // Assert
        assertThat(student.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Test
    void existsById_shouldReturnTrue_whenStudentExists() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    void isActive_shouldReturnFalse_whenStudentInactive() {
        SchoolStudent student = buildStudent(1L, 1L, 10L, null, StudentStatus.INACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        assertThat(service.isActive(1L)).isFalse();
    }

    @Test
    void isActive_shouldReturnFalse_whenStudentNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.isActive(99L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolStudent buildStudent(Long id, Long schoolId, Long personId,
                                        Long userId, StudentStatus status) {
        SchoolStudent s = SchoolStudent.builder()
                .schoolId(schoolId).personId(personId).userId(userId)
                .admissionNumber("ADM-001").enrollmentDate(LocalDate.now())
                .status(status).build();
        s.setId(id);
        return s;
    }

    private EnrollStudentRequest buildEnrollRequest(Long personId, String admissionNumber) {
        EnrollStudentRequest req = new EnrollStudentRequest();
        req.setPersonId(personId);
        req.setAdmissionNumber(admissionNumber);
        req.setEnrollmentDate(LocalDate.now());
        return req;
    }

    private UserResponse buildUserResponse(Long userId, Long personId) {
        return UserResponse.builder().id(userId).personId(personId).email("a@b.com")
                .username("ab").status(UserStatus.ACTIVE).mustChangePassword(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
