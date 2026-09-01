package com.attendai.school.settings.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Summary response for a single school setting entry in the list view.
 * Shows both the current school-level value and the default for comparison.
 */
@Getter
@Builder
public class SchoolSettingsSummaryResponse {
    private final String key;
    /** Current effective value for this school (may be a school override or the default). */
    private final String value;
    /** The module-level default value for this key. */
    private final String defaultValue;
    private final String description;
}
