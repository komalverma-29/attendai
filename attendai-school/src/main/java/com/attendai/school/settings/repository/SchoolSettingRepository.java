package com.attendai.school.settings.repository;

import com.attendai.school.settings.entity.SchoolSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolSettingRepository extends JpaRepository<SchoolSetting, Long> {

    /** Find a specific setting for a school by key. */
    Optional<SchoolSetting> findBySchoolIdAndSettingKey(Long schoolId, String settingKey);

    /** Find all settings for a school. */
    List<SchoolSetting> findBySchoolId(Long schoolId);

    /** Delete a specific setting for a school by key. */
    void deleteBySchoolIdAndSettingKey(Long schoolId, String settingKey);
}
