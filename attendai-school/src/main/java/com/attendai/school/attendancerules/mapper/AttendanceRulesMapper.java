package com.attendai.school.attendancerules.mapper;

import com.attendai.school.attendancerules.dto.AttendanceRuleSetResponse;
import com.attendai.school.attendancerules.dto.SectionOverrideResponse;
import com.attendai.school.attendancerules.entity.AttendanceRuleSet;
import com.attendai.school.attendancerules.entity.SectionAttendanceRuleOverride;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceRulesMapper {
    AttendanceRuleSetResponse  toResponse(AttendanceRuleSet ruleSet);
    SectionOverrideResponse    toOverrideResponse(SectionAttendanceRuleOverride override);
}
