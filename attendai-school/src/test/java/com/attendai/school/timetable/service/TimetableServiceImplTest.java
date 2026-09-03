package com.attendai.school.timetable.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.entity.SectionStatus;
import com.attendai.school.section.service.SchoolSectionService;
import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import com.attendai.school.teacherassignment.entity.TeacherAssignment;
import com.attendai.school.teacherassignment.repository.TeacherAssignmentRepository;
import com.attendai.school.timetable.dto.CreateTimeSlotRequest;
import com.attendai.school.timetable.dto.CreateTimetableEntryRequest;
import com.attendai.school.timetable.dto.UpdateTimetableEntryRequest;
import com.attendai.school.timetable.entity.SchoolTimeSlot;
import com.attendai.school.timetable.entity.TimeSlotType;
import com.attendai.school.timetable.entity.TimetableEntry;
import com.attendai.school.timetable.exception.TimetableEntryNotFoundException;
import com.attendai.school.timetable.mapper.TimetableMapper;
import com.attendai.school.timetable.repository.SchoolTimeSlotRepository;
import com.attendai.school.timetable.repository.TimetableEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceImplTest {

    @Mock SchoolTimeSlotRepository   timeSlotRepository;
    @Mock TimetableEntryRepository   entryRepository;
    @Mock TimetableMapper            timetableMapper;
    @Mock SchoolSectionService       sectionService;
    @Mock TeacherAssignmentRepository assignmentRepository;
    @Mock AuditService               auditService;

    private TimetableServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TimetableServiceImpl(
                timeSlotRepository, entryRepository, timetableMapper,
                sectionService, assignmentRepository, auditService);
    }

    // =========================================================================
    // createTimeSlot
    // =========================================================================

    @Test
    void createTimeSlot_shouldSave_whenValid() {
        when(timeSlotRepository.existsBySchoolIdAndName(1L, "Period 1")).thenReturn(false);
        SchoolTimeSlot saved = buildSlot(1L, 1L, "Period 1");
        when(timeSlotRepository.save(any())).thenReturn(saved);
        when(timetableMapper.toTimeSlotResponse(saved)).thenReturn(null);

        service.createTimeSlot(1L, buildSlotRequest("Period 1",
                LocalTime.of(9, 0), LocalTime.of(9, 45)));

        verify(timeSlotRepository).save(any(SchoolTimeSlot.class));
        verify(auditService).log(any());
    }

    @Test
    void createTimeSlot_shouldThrow409_whenNameDuplicate() {
        when(timeSlotRepository.existsBySchoolIdAndName(1L, "Period 1")).thenReturn(true);

        assertThatThrownBy(() -> service.createTimeSlot(1L,
                buildSlotRequest("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 45))))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createTimeSlot_shouldThrow_whenEndTimeNotAfterStartTime() {
        when(timeSlotRepository.existsBySchoolIdAndName(1L, "Bad")).thenReturn(false);

        assertThatThrownBy(() -> service.createTimeSlot(1L,
                buildSlotRequest("Bad", LocalTime.of(9, 45), LocalTime.of(9, 0))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    // =========================================================================
    // createEntry — happy path
    // =========================================================================

    @Test
    void createEntry_shouldSave_whenAllRulesPass() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        stubAssignment(10L, 1L, 20L, AssignmentStatus.ACTIVE);
        when(entryRepository.existsBySectionIdAndTimeSlotIdAndDayOfWeekAndAcademicYearId(
                20L, 5L, DayOfWeek.MONDAY, 100L)).thenReturn(false);
        when(entryRepository.existsByAssignmentIdAndTimeSlotIdAndDayOfWeek(
                10L, 5L, DayOfWeek.MONDAY)).thenReturn(false);
        TimetableEntry saved = buildEntry(1L, 1L, 100L, 20L, 5L, DayOfWeek.MONDAY, 10L);
        when(entryRepository.save(any())).thenReturn(saved);
        when(timetableMapper.toEntryResponse(saved)).thenReturn(null);

        service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L));

        verify(entryRepository).save(any(TimetableEntry.class));
        verify(auditService).log(any());
    }

    // =========================================================================
    // createEntry — validation failures
    // =========================================================================

    @Test
    void createEntry_shouldThrow_whenSectionBelongsToDifferentSchool() {
        when(sectionService.findById(20L)).thenReturn(
                buildSectionResponse(20L, 99L));  // school 99 != 1

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
        verify(entryRepository, never()).save(any());
    }

    @Test
    void createEntry_shouldThrow_whenAssignmentNotFound() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void createEntry_shouldThrow_whenAssignmentBelongsToDifferentSchool() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        // Assignment belongs to school 99
        TeacherAssignment a = buildAssignmentEntity(10L, 99L, 20L, AssignmentStatus.ACTIVE);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
    }

    @Test
    void createEntry_shouldThrow_whenAssignmentInactive() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        stubAssignment(10L, 1L, 20L, AssignmentStatus.INACTIVE);

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void createEntry_shouldThrow_whenAssignmentBelongsToDifferentSection() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        // Assignment belongs to section 99, not 20
        TeacherAssignment a = buildAssignmentEntity(10L, 1L, 99L, AssignmentStatus.ACTIVE);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to section");
    }

    @Test
    void createEntry_shouldThrow409_whenSectionSlotDayAlreadyExists() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        stubAssignment(10L, 1L, 20L, AssignmentStatus.ACTIVE);
        when(entryRepository.existsBySectionIdAndTimeSlotIdAndDayOfWeekAndAcademicYearId(
                20L, 5L, DayOfWeek.MONDAY, 100L)).thenReturn(true);

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("already exists for section");
    }

    @Test
    void createEntry_shouldThrow409_whenTeacherAlreadyScheduledAtSameSlot() {
        stubSectionInSchool(20L, 1L);
        stubSlotInSchool(5L, 1L);
        stubAssignment(10L, 1L, 20L, AssignmentStatus.ACTIVE);
        when(entryRepository.existsBySectionIdAndTimeSlotIdAndDayOfWeekAndAcademicYearId(
                20L, 5L, DayOfWeek.MONDAY, 100L)).thenReturn(false);
        // Teacher conflict — same assignment already in another section
        when(entryRepository.existsByAssignmentIdAndTimeSlotIdAndDayOfWeek(
                10L, 5L, DayOfWeek.MONDAY)).thenReturn(true);

        assertThatThrownBy(() ->
                service.createEntry(1L, 100L, buildEntryRequest(20L, 5L, DayOfWeek.MONDAY, 10L)))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Teacher is already scheduled");
    }

    // =========================================================================
    // deleteEntry
    // =========================================================================

    @Test
    void deleteEntry_shouldSoftDelete_whenFound() {
        TimetableEntry entry = buildEntry(1L, 1L, 100L, 20L, 5L, DayOfWeek.MONDAY, 10L);
        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any())).thenReturn(entry);

        service.deleteEntry(1L, 1L);

        assertThat(entry.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    @Test
    void deleteEntry_shouldThrow404_whenNotFound() {
        when(entryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteEntry(1L, 99L))
                .isInstanceOf(TimetableEntryNotFoundException.class);
    }

    @Test
    void deleteEntry_shouldThrow404_whenBelongsToDifferentSchool() {
        TimetableEntry entry = buildEntry(1L, 2L, 100L, 20L, 5L, DayOfWeek.MONDAY, 10L);
        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));
        assertThatThrownBy(() -> service.deleteEntry(1L, 1L))
                .isInstanceOf(TimetableEntryNotFoundException.class);
    }

    // =========================================================================
    // Internal APIs
    // =========================================================================

    @Test
    void hasTimetableEntry_shouldReturnTrue_whenEntriesExist() {
        when(entryRepository.countBySectionIdAndAcademicYearId(20L, 100L)).thenReturn(3L);
        assertThat(service.hasTimetableEntry(20L, 100L)).isTrue();
    }

    @Test
    void hasTimetableEntry_shouldReturnFalse_whenNoEntries() {
        when(entryRepository.countBySectionIdAndAcademicYearId(20L, 100L)).thenReturn(0L);
        assertThat(service.hasTimetableEntry(20L, 100L)).isFalse();
    }

    @Test
    void getSubjectsForSectionOnDay_shouldReturnSubjectInfoList() {
        TimetableEntry e = buildEntry(1L, 1L, 100L, 20L, 5L, DayOfWeek.MONDAY, 10L);
        when(entryRepository.findBySectionIdAndAcademicYearIdAndDayOfWeek(
                20L, 100L, DayOfWeek.MONDAY)).thenReturn(List.of(e));
        TeacherAssignment a = buildAssignmentEntity(10L, 1L, 20L, AssignmentStatus.ACTIVE);
        a.setSubjectId(30L);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(a));

        var result = service.getSubjectsForSectionOnDay(20L, 100L, DayOfWeek.MONDAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubjectId()).isEqualTo(30L);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubSectionInSchool(Long sectionId, Long schoolId) {
        when(sectionService.findById(sectionId))
                .thenReturn(buildSectionResponse(sectionId, schoolId));
    }

    private void stubSlotInSchool(Long slotId, Long schoolId) {
        SchoolTimeSlot slot = buildSlot(slotId, schoolId, "Period 1");
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
    }

    private void stubAssignment(Long id, Long schoolId, Long sectionId,
                                 AssignmentStatus status) {
        TeacherAssignment a = buildAssignmentEntity(id, schoolId, sectionId, status);
        when(assignmentRepository.findById(id)).thenReturn(Optional.of(a));
    }

    private SectionResponse buildSectionResponse(Long id, Long schoolId) {
        return SectionResponse.builder()
                .id(id).schoolId(schoolId).classId(5L).academicYearId(100L)
                .name("A").status(SectionStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private SchoolTimeSlot buildSlot(Long id, Long schoolId, String name) {
        SchoolTimeSlot s = SchoolTimeSlot.builder()
                .schoolId(schoolId).name(name)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 45))
                .slotOrder(1).slotType(TimeSlotType.PERIOD).build();
        s.setId(id);
        return s;
    }

    private TeacherAssignment buildAssignmentEntity(Long id, Long schoolId, Long sectionId,
                                                     AssignmentStatus status) {
        TeacherAssignment a = TeacherAssignment.builder()
                .schoolId(schoolId).sectionId(sectionId).subjectId(30L)
                .teacherId(40L).academicYearId(100L).status(status).build();
        a.setId(id);
        return a;
    }

    private TimetableEntry buildEntry(Long id, Long schoolId, Long yearId,
                                       Long sectionId, Long slotId,
                                       DayOfWeek day, Long assignmentId) {
        TimetableEntry e = TimetableEntry.builder()
                .schoolId(schoolId).academicYearId(yearId).sectionId(sectionId)
                .timeSlotId(slotId).dayOfWeek(day).assignmentId(assignmentId).build();
        e.setId(id);
        return e;
    }

    private CreateTimeSlotRequest buildSlotRequest(String name, LocalTime start, LocalTime end) {
        CreateTimeSlotRequest req = new CreateTimeSlotRequest();
        req.setName(name); req.setStartTime(start); req.setEndTime(end);
        req.setSlotOrder(1);
        return req;
    }

    private CreateTimetableEntryRequest buildEntryRequest(Long sectionId, Long slotId,
                                                           DayOfWeek day, Long assignmentId) {
        CreateTimetableEntryRequest req = new CreateTimetableEntryRequest();
        req.setSectionId(sectionId); req.setTimeSlotId(slotId);
        req.setDayOfWeek(day); req.setAssignmentId(assignmentId);
        return req;
    }
}
