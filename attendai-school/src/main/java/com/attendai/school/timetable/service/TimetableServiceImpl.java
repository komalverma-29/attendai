package com.attendai.school.timetable.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.section.service.SchoolSectionService;
import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import com.attendai.school.teacherassignment.repository.TeacherAssignmentRepository;
import com.attendai.school.timetable.dto.CreateTimeSlotRequest;
import com.attendai.school.timetable.dto.CreateTimetableEntryRequest;
import com.attendai.school.timetable.dto.SectionTimetableResponse;
import com.attendai.school.timetable.dto.SubjectPeriodInfo;
import com.attendai.school.timetable.dto.TimeSlotResponse;
import com.attendai.school.timetable.dto.TimetableEntryResponse;
import com.attendai.school.timetable.dto.UpdateTimetableEntryRequest;
import com.attendai.school.timetable.entity.SchoolTimeSlot;
import com.attendai.school.timetable.entity.TimetableEntry;
import com.attendai.school.timetable.exception.TimetableEntryNotFoundException;
import com.attendai.school.timetable.mapper.TimetableMapper;
import com.attendai.school.timetable.repository.SchoolTimeSlotRepository;
import com.attendai.school.timetable.repository.TimetableEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private static final String MODULE = "school";

    private final SchoolTimeSlotRepository   timeSlotRepository;
    private final TimetableEntryRepository   entryRepository;
    private final TimetableMapper            timetableMapper;
    private final SchoolSectionService       sectionService;
    private final TeacherAssignmentRepository assignmentRepository;
    private final AuditService               auditService;

    // =========================================================================
    // Time Slots
    // =========================================================================

    @Override
    @Transactional
    public TimeSlotResponse createTimeSlot(Long schoolId, CreateTimeSlotRequest request) {
        if (timeSlotRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "Time slot '" + request.getName() + "' already exists in school " + schoolId);
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ValidationException("End time must be after start time");
        }

        SchoolTimeSlot slot = SchoolTimeSlot.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .slotOrder(request.getSlotOrder())
                .slotType(request.getSlotType() != null
                        ? request.getSlotType()
                        : com.attendai.school.timetable.entity.TimeSlotType.PERIOD)
                .build();

        SchoolTimeSlot saved = timeSlotRepository.save(slot);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TIME_SLOT_CREATED")
                .module(MODULE).resourceType("SchoolTimeSlot")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId + ",\"name\":\"" + request.getName() + "\"}")
                .build());

        return timetableMapper.toTimeSlotResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> listTimeSlots(Long schoolId) {
        return timeSlotRepository.findBySchoolIdOrderBySlotOrderAsc(schoolId)
                .stream()
                .map(timetableMapper::toTimeSlotResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTimeSlot(Long schoolId, Long timeSlotId) {
        SchoolTimeSlot slot = requireTimeSlot(schoolId, timeSlotId);
        slot.softDelete();
        timeSlotRepository.save(slot);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TIME_SLOT_DELETED")
                .module(MODULE).resourceType("SchoolTimeSlot")
                .resourceId(String.valueOf(timeSlotId)).build());
    }

    // =========================================================================
    // Timetable Entries
    // =========================================================================

    @Override
    @Transactional
    public TimetableEntryResponse createEntry(Long schoolId, Long academicYearId,
                                               CreateTimetableEntryRequest request) {
        // Validate section belongs to this school
        var section = sectionService.findById(request.getSectionId());
        if (!section.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Section " + request.getSectionId()
                    + " does not belong to school " + schoolId);
        }

        // Validate time slot belongs to this school
        requireTimeSlot(schoolId, request.getTimeSlotId());

        // Validate assignment: exists, belongs to this school, and is ACTIVE (BR-TT-03)
        var assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ValidationException(
                        "Assignment " + request.getAssignmentId() + " was not found"));
        if (!assignment.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Assignment " + request.getAssignmentId()
                    + " does not belong to school " + schoolId);
        }
        if (!AssignmentStatus.ACTIVE.equals(assignment.getStatus())) {
            throw new ValidationException(
                    "Assignment " + request.getAssignmentId() + " is not ACTIVE");
        }
        // Assignment must belong to the same section
        if (!assignment.getSectionId().equals(request.getSectionId())) {
            throw new ValidationException(
                    "Assignment " + request.getAssignmentId()
                    + " does not belong to section " + request.getSectionId());
        }

        // BR-TT-01: one entry per section × slot × day × year
        if (entryRepository.existsBySectionIdAndTimeSlotIdAndDayOfWeekAndAcademicYearId(
                request.getSectionId(), request.getTimeSlotId(),
                request.getDayOfWeek(), academicYearId)) {
            throw new ResourceAlreadyExistsException(
                    "A timetable entry already exists for section " + request.getSectionId()
                    + " on " + request.getDayOfWeek()
                    + " at time slot " + request.getTimeSlotId());
        }

        // BR-TT-02: teacher cannot be in two sections at the same slot+day
        if (entryRepository.existsByAssignmentIdAndTimeSlotIdAndDayOfWeek(
                request.getAssignmentId(), request.getTimeSlotId(), request.getDayOfWeek())) {
            throw new ResourceAlreadyExistsException(
                    "Teacher is already scheduled in a different section on "
                    + request.getDayOfWeek() + " at time slot " + request.getTimeSlotId());
        }

        TimetableEntry entry = TimetableEntry.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .sectionId(request.getSectionId())
                .timeSlotId(request.getTimeSlotId())
                .dayOfWeek(request.getDayOfWeek())
                .assignmentId(request.getAssignmentId())
                .build();

        TimetableEntry saved = entryRepository.save(entry);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TIMETABLE_ENTRY_CREATED")
                .module(MODULE).resourceType("TimetableEntry")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"sectionId\":" + request.getSectionId()
                         + ",\"dayOfWeek\":\"" + request.getDayOfWeek() + "\"}")
                .build());

        return timetableMapper.toEntryResponse(saved);
    }

    @Override
    @Transactional
    public TimetableEntryResponse updateEntry(Long schoolId, Long entryId,
                                               UpdateTimetableEntryRequest request) {
        TimetableEntry entry = requireEntry(schoolId, entryId);

        if (request.getAssignmentId() != null) {
            var assignment = assignmentRepository.findById(request.getAssignmentId())
                    .orElseThrow(() -> new ValidationException(
                            "Assignment " + request.getAssignmentId() + " was not found"));
            if (!assignment.getSchoolId().equals(schoolId)) {
                throw new ValidationException(
                        "Assignment " + request.getAssignmentId()
                        + " does not belong to school " + schoolId);
            }
            if (!AssignmentStatus.ACTIVE.equals(assignment.getStatus())) {
                throw new ValidationException(
                        "Assignment " + request.getAssignmentId() + " is not ACTIVE");
            }
            if (!assignment.getSectionId().equals(entry.getSectionId())) {
                throw new ValidationException(
                        "Assignment " + request.getAssignmentId()
                        + " does not belong to section " + entry.getSectionId());
            }
            // BR-TT-02: no teacher conflict with the new assignment
            if (entryRepository.existsByAssignmentIdAndTimeSlotIdAndDayOfWeek(
                    request.getAssignmentId(), entry.getTimeSlotId(), entry.getDayOfWeek())) {
                // Check that the existing conflict is not just the entry itself
                // (assignment changes within same entry are fine)
                boolean sameEntry = entryRepository
                        .findByAssignmentId(request.getAssignmentId())
                        .stream()
                        .anyMatch(e -> e.getId().equals(entryId));
                if (!sameEntry) {
                    throw new ResourceAlreadyExistsException(
                            "New assignment's teacher is already scheduled on "
                            + entry.getDayOfWeek()
                            + " at time slot " + entry.getTimeSlotId());
                }
            }
            entry.setAssignmentId(request.getAssignmentId());
        }

        TimetableEntry saved = entryRepository.save(entry);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TIMETABLE_ENTRY_UPDATED")
                .module(MODULE).resourceType("TimetableEntry")
                .resourceId(String.valueOf(entryId)).build());

        return timetableMapper.toEntryResponse(saved);
    }

    @Override
    @Transactional
    public void deleteEntry(Long schoolId, Long entryId) {
        TimetableEntry entry = requireEntry(schoolId, entryId);
        entry.softDelete();
        entryRepository.save(entry);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TIMETABLE_ENTRY_DELETED")
                .module(MODULE).resourceType("TimetableEntry")
                .resourceId(String.valueOf(entryId)).build());
    }

    // =========================================================================
    // Views
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SectionTimetableResponse getSectionTimetable(Long schoolId, Long sectionId,
                                                         Long academicYearId) {
        // Validate section belongs to school
        var section = sectionService.findById(sectionId);
        if (!section.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Section " + sectionId + " does not belong to school " + schoolId);
        }

        List<TimetableEntry> entries =
                entryRepository.findBySectionIdAndAcademicYearId(sectionId, academicYearId);

        Map<DayOfWeek, List<TimetableEntryResponse>> schedule = new EnumMap<>(DayOfWeek.class);
        entries.stream()
                .collect(Collectors.groupingBy(TimetableEntry::getDayOfWeek))
                .forEach((day, dayEntries) ->
                        schedule.put(day, dayEntries.stream()
                                .map(timetableMapper::toEntryResponse)
                                .toList()));

        return SectionTimetableResponse.builder()
                .sectionId(sectionId)
                .academicYearId(academicYearId)
                .schedule(schedule)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getTeacherTimetable(Long schoolId, Long teacherId,
                                                             Long academicYearId) {
        return entryRepository.findByTeacherIdAndAcademicYearId(teacherId, academicYearId)
                .stream()
                .filter(e -> e.getSchoolId().equals(schoolId))
                .map(timetableMapper::toEntryResponse)
                .toList();
    }

    // =========================================================================
    // Internal APIs
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SubjectPeriodInfo> getSubjectsForSectionOnDay(Long sectionId,
                                                               Long academicYearId,
                                                               DayOfWeek day) {
        return entryRepository
                .findBySectionIdAndAcademicYearIdAndDayOfWeek(sectionId, academicYearId, day)
                .stream()
                .map(e -> {
                    Long subjectId = assignmentRepository.findById(e.getAssignmentId())
                            .map(a -> a.getSubjectId())
                            .orElse(null);
                    return SubjectPeriodInfo.builder()
                            .subjectId(subjectId)
                            .assignmentId(e.getAssignmentId())
                            .timeSlotId(e.getTimeSlotId())
                            .build();
                })
                .filter(info -> info.getSubjectId() != null)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTimetableEntry(Long sectionId, Long academicYearId) {
        return entryRepository.countBySectionIdAndAcademicYearId(sectionId, academicYearId) > 0;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SchoolTimeSlot requireTimeSlot(Long schoolId, Long timeSlotId) {
        SchoolTimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new com.attendai.core.common.exception.ResourceNotFoundException(
                        "Time slot with id " + timeSlotId + " was not found"));
        if (!slot.getSchoolId().equals(schoolId)) {
            throw new com.attendai.core.common.exception.ResourceNotFoundException(
                    "Time slot with id " + timeSlotId + " was not found");
        }
        return slot;
    }

    private TimetableEntry requireEntry(Long schoolId, Long entryId) {
        TimetableEntry e = entryRepository.findById(entryId)
                .orElseThrow(() -> new TimetableEntryNotFoundException(entryId));
        if (!e.getSchoolId().equals(schoolId))
            throw new TimetableEntryNotFoundException(entryId);
        return e;
    }
}
