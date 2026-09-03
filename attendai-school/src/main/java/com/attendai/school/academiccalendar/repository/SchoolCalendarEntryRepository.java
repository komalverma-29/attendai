package com.attendai.school.academiccalendar.repository;

import com.attendai.school.academiccalendar.entity.CalendarEntryType;
import com.attendai.school.academiccalendar.entity.SchoolCalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolCalendarEntryRepository extends JpaRepository<SchoolCalendarEntry, Long> {

    /**
     * BR-CAL-01: uniqueness check before insert (duplicate date within school+year).
     */
    boolean existsBySchoolIdAndAcademicYearIdAndEntryDate(
            Long schoolId, Long academicYearId, LocalDate entryDate);

    /**
     * Fetch a single entry for a specific date (used by isWorkingDay logic).
     */
    Optional<SchoolCalendarEntry> findBySchoolIdAndAcademicYearIdAndEntryDate(
            Long schoolId, Long academicYearId, LocalDate entryDate);

    /**
     * All entries for a school+year — loaded into cache and used for bulk processing.
     */
    List<SchoolCalendarEntry> findBySchoolIdAndAcademicYearId(
            Long schoolId, Long academicYearId);

    /**
     * Date-range query for a month calendar view or working-day count.
     */
    @Query("""
            SELECT e FROM SchoolCalendarEntry e
            WHERE e.schoolId       = :schoolId
              AND e.academicYearId = :academicYearId
              AND e.entryDate      >= :fromDate
              AND e.entryDate      <= :toDate
            ORDER BY e.entryDate ASC
            """)
    List<SchoolCalendarEntry> findBySchoolIdAndAcademicYearIdAndDateBetween(
            @Param("schoolId")       Long      schoolId,
            @Param("academicYearId") Long      academicYearId,
            @Param("fromDate")       LocalDate fromDate,
            @Param("toDate")         LocalDate toDate);

    /**
     * Filtered listing with optional type filter.
     */
    @Query("""
            SELECT e FROM SchoolCalendarEntry e
            WHERE e.schoolId       = :schoolId
              AND e.academicYearId = :academicYearId
              AND (:entryType IS NULL OR e.entryType = :entryType)
              AND (:fromDate  IS NULL OR e.entryDate >= :fromDate)
              AND (:toDate    IS NULL OR e.entryDate <= :toDate)
            ORDER BY e.entryDate ASC
            """)
    List<SchoolCalendarEntry> findByFilters(
            @Param("schoolId")       Long              schoolId,
            @Param("academicYearId") Long              academicYearId,
            @Param("entryType")      CalendarEntryType entryType,
            @Param("fromDate")       LocalDate         fromDate,
            @Param("toDate")         LocalDate         toDate);
}
