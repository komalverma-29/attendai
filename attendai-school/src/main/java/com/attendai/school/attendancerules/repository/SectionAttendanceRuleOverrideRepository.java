package com.attendai.school.attendancerules.repository;

import com.attendai.school.attendancerules.entity.SectionAttendanceRuleOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectionAttendanceRuleOverrideRepository
        extends JpaRepository<SectionAttendanceRuleOverride, Long> {

    /** BR-RULES-02: check before creating an override. */
    boolean existsByRuleSetIdAndSectionId(Long ruleSetId, Long sectionId);

    /** Primary lookup for an override. */
    Optional<SectionAttendanceRuleOverride> findByRuleSetIdAndSectionId(
            Long ruleSetId, Long sectionId);

    /** Deletion by rule-set + section (used by FR-RULES-05). */
    void deleteByRuleSetIdAndSectionId(Long ruleSetId, Long sectionId);
}
