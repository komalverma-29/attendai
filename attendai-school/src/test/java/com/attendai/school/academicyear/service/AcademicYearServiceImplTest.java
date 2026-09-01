package com.attendai.school.academicyear.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.dto.CreateAcademicYearRequest;
import com.attendai.school.academicyear.dto.UpdateAcademicYearRequest;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.entity.SchoolAcademicYear;
import com.attendai.school.academicyear.exception.AcademicYearNotFoundException;
import com.attendai.school.academicyear.exception.ActiveAcademicYearAlreadyExistsException;
import com.attendai.school.academicyear.mapper.AcademicYearMapper;
import com.attendai.school.academicyear.repository.SchoolAcademicYearRepository;
import com.attendai.school.school.service.SchoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceImplTest {

    @Mock SchoolAcademicYearRepository yearRepository;
    @Mock AcademicYearMapper           yearMapper;
    @Mock SchoolService                schoolService;
    @Mock AuditService                 auditService;

    private AcademicYearServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AcademicYearServiceImpl(yearRepository, yearMapper, schoolService, auditService);
    }

    // -------------------------------------------------------------------------
    // createAcademicYear
    // -------------------------------------------------------------------------

    @Test
    void createAcademicYear_shouldSave_whenValidRequest() {
        // Arrange
        when(schoolService.isActive(1L)).thenReturn(true);
        when(yearRepository.existsBySchoolIdAndName(1L, "2025-2026")).thenReturn(false);
        when(yearRepository.findOverlapping(eq(1L), anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        SchoolAcademicYear saved = buildYear(1L, 1L, "2025-2026", AcademicYearStatus.UPCOMING);
        when(yearRepository.save(any())).thenReturn(saved);
        when(yearMapper.toResponse(saved)).thenReturn(null);

        // Act
        service.createAcademicYear(1L, buildCreateRequest("2025-2026",
                LocalDate.of(2025, 6, 1), LocalDate.of(2026, 3, 31)));

        // Assert
        verify(yearRepository).save(any(SchoolAcademicYear.class));
        verify(auditService).log(any());
    }

    @Test
    void createAcademicYear_shouldThrow_whenSchoolNotActive() {
        when(schoolService.isActive(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.createAcademicYear(1L,
                buildCreateRequest("2025-2026",
                        LocalDate.of(2025, 6, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ValidationException.class);
        verify(yearRepository, never()).save(any());
    }

    @Test
    void createAcademicYear_shouldThrow_whenEndDateBeforeStartDate() {
        when(schoolService.isActive(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createAcademicYear(1L,
                buildCreateRequest("BAD",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2025, 3, 31))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End date must be after start date");
    }

    @Test
    void createAcademicYear_shouldThrow409_whenNameDuplicate() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(yearRepository.existsBySchoolIdAndName(1L, "2025-2026")).thenReturn(true);

        assertThatThrownBy(() -> service.createAcademicYear(1L,
                buildCreateRequest("2025-2026",
                        LocalDate.of(2025, 6, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createAcademicYear_shouldThrow_whenDateRangeOverlaps() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(yearRepository.existsBySchoolIdAndName(1L, "2025-2026")).thenReturn(false);
        when(yearRepository.findOverlapping(eq(1L), anyLong(), any(), any()))
                .thenReturn(List.of(buildYear(2L, 1L, "OLD", AcademicYearStatus.ACTIVE)));

        assertThatThrownBy(() -> service.createAcademicYear(1L,
                buildCreateRequest("2025-2026",
                        LocalDate.of(2025, 6, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("overlaps");
    }

    // -------------------------------------------------------------------------
    // activateAcademicYear
    // -------------------------------------------------------------------------

    @Test
    void activateAcademicYear_shouldActivate_whenNoActiveYearExists() {
        SchoolAcademicYear upcoming = buildYear(1L, 1L, "2025-2026", AcademicYearStatus.UPCOMING);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        when(yearRepository.findBySchoolIdAndStatus(1L, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(yearRepository.save(any())).thenReturn(upcoming);
        when(yearMapper.toResponse(any())).thenReturn(null);

        service.activateAcademicYear(1L, 1L);

        assertThat(upcoming.getStatus()).isEqualTo(AcademicYearStatus.ACTIVE);
    }

    @Test
    void activateAcademicYear_shouldThrow409_whenAnotherYearAlreadyActive() {
        SchoolAcademicYear upcoming = buildYear(1L, 1L, "2025-2026", AcademicYearStatus.UPCOMING);
        SchoolAcademicYear active   = buildYear(2L, 1L, "2024-2025", AcademicYearStatus.ACTIVE);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        when(yearRepository.findBySchoolIdAndStatus(1L, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.activateAcademicYear(1L, 1L))
                .isInstanceOf(ActiveAcademicYearAlreadyExistsException.class);
    }

    @Test
    void activateAcademicYear_shouldThrow_whenYearNotUpcoming() {
        SchoolAcademicYear cancelled = buildYear(1L, 1L, "X", AcademicYearStatus.CANCELLED);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.activateAcademicYear(1L, 1L))
                .isInstanceOf(ValidationException.class);
    }

    // -------------------------------------------------------------------------
    // completeAcademicYear
    // -------------------------------------------------------------------------

    @Test
    void completeAcademicYear_shouldComplete_whenActive() {
        SchoolAcademicYear active = buildYear(1L, 1L, "2025-2026", AcademicYearStatus.ACTIVE);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(active));
        when(yearRepository.save(any())).thenReturn(active);
        when(yearMapper.toResponse(any())).thenReturn(null);

        service.completeAcademicYear(1L, 1L);

        assertThat(active.getStatus()).isEqualTo(AcademicYearStatus.COMPLETED);
    }

    @Test
    void completeAcademicYear_shouldThrow_whenNotActive() {
        SchoolAcademicYear upcoming = buildYear(1L, 1L, "X", AcademicYearStatus.UPCOMING);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(upcoming));

        assertThatThrownBy(() -> service.completeAcademicYear(1L, 1L))
                .isInstanceOf(ValidationException.class);
    }

    // -------------------------------------------------------------------------
    // cancelAcademicYear
    // -------------------------------------------------------------------------

    @Test
    void cancelAcademicYear_shouldCancel_whenUpcoming() {
        SchoolAcademicYear upcoming = buildYear(1L, 1L, "X", AcademicYearStatus.UPCOMING);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        when(yearRepository.save(any())).thenReturn(upcoming);
        when(yearMapper.toResponse(any())).thenReturn(null);

        service.cancelAcademicYear(1L, 1L);

        assertThat(upcoming.getStatus()).isEqualTo(AcademicYearStatus.CANCELLED);
    }

    @Test
    void cancelAcademicYear_shouldThrow_whenActive() {
        SchoolAcademicYear active = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.cancelAcademicYear(1L, 1L))
                .isInstanceOf(ValidationException.class);
    }

    // -------------------------------------------------------------------------
    // updateAcademicYear
    // -------------------------------------------------------------------------

    @Test
    void updateAcademicYear_shouldThrow_whenCompletedYear() {
        SchoolAcademicYear completed = buildYear(1L, 1L, "X", AcademicYearStatus.COMPLETED);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(completed));

        UpdateAcademicYearRequest req = new UpdateAcademicYearRequest();
        req.setName("New Name");

        assertThatThrownBy(() -> service.updateAcademicYear(1L, 1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("immutable");
    }

    @Test
    void updateAcademicYear_shouldThrow_whenDateChangeOnActiveYear() {
        SchoolAcademicYear active = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(active));

        UpdateAcademicYearRequest req = new UpdateAcademicYearRequest();
        req.setStartDate(LocalDate.of(2025, 1, 1));

        assertThatThrownBy(() -> service.updateAcademicYear(1L, 1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("UPCOMING");
    }

    // -------------------------------------------------------------------------
    // deleteAcademicYear
    // -------------------------------------------------------------------------

    @Test
    void deleteAcademicYear_shouldSoftDelete_whenUpcoming() {
        SchoolAcademicYear upcoming = buildYear(1L, 1L, "X", AcademicYearStatus.UPCOMING);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        when(yearRepository.save(any())).thenReturn(upcoming);

        service.deleteAcademicYear(1L, 1L);

        assertThat(upcoming.isDeleted()).isTrue();
    }

    @Test
    void deleteAcademicYear_shouldThrow_whenActive() {
        SchoolAcademicYear active = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.deleteAcademicYear(1L, 1L))
                .isInstanceOf(ValidationException.class);
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Test
    void isDateWithinAcademicYear_shouldReturnTrue_whenDateOnStartBoundary() {
        SchoolAcademicYear year = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        year.setStartDate(LocalDate.of(2025, 6, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));

        assertThat(service.isDateWithinAcademicYear(1L, LocalDate.of(2025, 6, 1))).isTrue();
    }

    @Test
    void isDateWithinAcademicYear_shouldReturnTrue_whenDateOnEndBoundary() {
        SchoolAcademicYear year = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        year.setStartDate(LocalDate.of(2025, 6, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));

        assertThat(service.isDateWithinAcademicYear(1L, LocalDate.of(2026, 3, 31))).isTrue();
    }

    @Test
    void isDateWithinAcademicYear_shouldReturnFalse_whenDateBeforeStart() {
        SchoolAcademicYear year = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        year.setStartDate(LocalDate.of(2025, 6, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));

        assertThat(service.isDateWithinAcademicYear(1L, LocalDate.of(2025, 5, 31))).isFalse();
    }

    @Test
    void isDateWithinAcademicYear_shouldReturnFalse_whenDateAfterEnd() {
        SchoolAcademicYear year = buildYear(1L, 1L, "X", AcademicYearStatus.ACTIVE);
        year.setStartDate(LocalDate.of(2025, 6, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));

        assertThat(service.isDateWithinAcademicYear(1L, LocalDate.of(2026, 4, 1))).isFalse();
    }

    @Test
    void getActiveAcademicYearOrThrow_shouldThrow404_whenNoActiveYear() {
        when(yearRepository.findBySchoolIdAndStatus(1L, AcademicYearStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveAcademicYearOrThrow(1L))
                .isInstanceOf(AcademicYearNotFoundException.class)
                .hasMessageContaining("No ACTIVE");
    }

    @Test
    void findById_shouldThrow404_whenYearBelongsToDifferentSchool() {
        SchoolAcademicYear year = buildYear(1L, 2L, "X", AcademicYearStatus.UPCOMING);
        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));

        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(AcademicYearNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolAcademicYear buildYear(Long id, Long schoolId, String name,
                                          AcademicYearStatus status) {
        SchoolAcademicYear y = SchoolAcademicYear.builder()
                .schoolId(schoolId).name(name).status(status)
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();
        y.setId(id);
        return y;
    }

    private CreateAcademicYearRequest buildCreateRequest(String name,
                                                          LocalDate start, LocalDate end) {
        CreateAcademicYearRequest req = new CreateAcademicYearRequest();
        req.setName(name);
        req.setStartDate(start);
        req.setEndDate(end);
        return req;
    }
}
