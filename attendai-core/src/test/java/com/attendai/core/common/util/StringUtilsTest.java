package com.attendai.core.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void isBlank_shouldReturnTrue_forNull() {
        assertThat(StringUtils.isBlank(null)).isTrue();
    }

    @Test
    void isBlank_shouldReturnTrue_forEmptyString() {
        assertThat(StringUtils.isBlank("")).isTrue();
    }

    @Test
    void isBlank_shouldReturnTrue_forWhitespaceOnly() {
        assertThat(StringUtils.isBlank("   ")).isTrue();
    }

    @Test
    void isBlank_shouldReturnFalse_forNonBlankString() {
        assertThat(StringUtils.isBlank("hello")).isFalse();
    }

    @Test
    void isNotBlank_shouldReturnTrue_forNonBlankString() {
        assertThat(StringUtils.isNotBlank("hello")).isTrue();
    }

    @Test
    void normalise_shouldTrimAndLowercase() {
        assertThat(StringUtils.normalise("  HELLO World  ")).isEqualTo("hello world");
    }

    @Test
    void normalise_shouldReturnEmptyString_forNull() {
        assertThat(StringUtils.normalise(null)).isEqualTo("");
    }

    @Test
    void toUpperCase_shouldTrimAndUppercase() {
        assertThat(StringUtils.toUpperCase("  hello  ")).isEqualTo("HELLO");
    }

    @Test
    void truncate_shouldNotTruncate_whenWithinLimit() {
        assertThat(StringUtils.truncate("hello", 10)).isEqualTo("hello");
    }

    @Test
    void truncate_shouldTruncate_whenExceedsLimit() {
        assertThat(StringUtils.truncate("hello world", 5)).isEqualTo("hello");
    }

    @Test
    void truncate_shouldReturnNull_forNull() {
        assertThat(StringUtils.truncate(null, 10)).isNull();
    }

    @Test
    void buildFullName_shouldJoinNonBlankParts() {
        assertThat(StringUtils.buildFullName("John", "Michael", "Doe"))
                .isEqualTo("John Michael Doe");
    }

    @Test
    void buildFullName_shouldSkipBlankParts() {
        assertThat(StringUtils.buildFullName("John", null, "Doe"))
                .isEqualTo("John Doe");
        assertThat(StringUtils.buildFullName("John", "  ", "Doe"))
                .isEqualTo("John Doe");
    }

    @Test
    void buildFullName_shouldHandleAllNull() {
        assertThat(StringUtils.buildFullName(null, null)).isEqualTo("");
    }

    @Test
    void sanitiseFilename_shouldReplacePathSeparators() {
        assertThat(StringUtils.sanitiseFilename("../etc/passwd")).isEqualTo(".._etc_passwd");
        assertThat(StringUtils.sanitiseFilename("file\\name.jpg")).isEqualTo("file_name.jpg");
    }

    @Test
    void sanitiseFilename_shouldReturnNull_forNull() {
        assertThat(StringUtils.sanitiseFilename(null)).isNull();
    }
}
