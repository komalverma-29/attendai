package com.attendai.school.subject.repository;

import com.attendai.school.subject.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    boolean existsByClassIdAndSubjectId(Long classId, Long subjectId);

    Optional<ClassSubject> findByClassIdAndSubjectId(Long classId, Long subjectId);

    /** Counts how many classes a subject is currently assigned to. */
    long countBySubjectId(Long subjectId);
}
