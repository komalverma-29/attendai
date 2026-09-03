package com.attendai.school.timetable.repository;

import com.attendai.school.timetable.entity.SchoolTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolTimeSlotRepository extends JpaRepository<SchoolTimeSlot, Long> {

    boolean existsBySchoolIdAndName(Long schoolId, String name);

    /** All time slots for a school ordered by slotOrder ascending. */
    List<SchoolTimeSlot> findBySchoolIdOrderBySlotOrderAsc(Long schoolId);
}
