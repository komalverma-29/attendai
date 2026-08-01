package com.attendai.core.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void formatForLog_shouldFormatDateTimeCorrectly() {
        LocalDateTime dt = LocalDateTime.of(2025, 1, 15, 10, 30, 45);
        assertThat(DateUtils.formatForLog(dt)).isEqualTo("2025-01-15T10:30:45");
    }

    @Test
    void formatForLog_shouldReturnNull_forNullInput() {
        assertThat(DateUtils.formatForLog(null)).isEqualTo("null");
    }

    @Test
    void formatDate_shouldFormatDateCorrectly() {
        LocalDate date = LocalDate.of(2025, 6, 1);
        assertThat(DateUtils.formatDate(date)).isEqualTo("2025-06-01");
    }

    @Test
    void isPastOrToday_shouldReturnTrue_forPastDate() {
        assertThat(DateUtils.isPastOrToday(LocalDate.now().minusDays(1))).isTrue();
    }

    @Test
    void isPastOrToday_shouldReturnTrue_forToday() {
        assertThat(DateUtils.isPastOrToday(LocalDate.now())).isTrue();
    }

    @Test
    void isPastOrToday_shouldReturnFalse_forFutureDate() {
        assertThat(DateUtils.isPastOrToday(LocalDate.now().plusDays(1))).isFalse();
    }

    @Test
    void isPastOrToday_shouldReturnFalse_forNull() {
        assertThat(DateUtils.isPastOrToday(null)).isFalse();
    }

    @Test
    void isWithinAllowedFutureWindow_shouldReturnTrue_forNow() {
        assertThat(DateUtils.isWithinAllowedFutureWindow(LocalDateTime.now())).isTrue();
    }

    @Test
    void isWithinAllowedFutureWindow_shouldReturnFalse_forFarFuture() {
        assertThat(DateUtils.isWithinAllowedFutureWindow(LocalDateTime.now().plusMinutes(10))).isFalse();
    }

    @Test
    void isWithinAllowedFutureWindow_shouldReturnFalse_forNull() {
        assertThat(DateUtils.isWithinAllowedFutureWindow(null)).isFalse();
    }

    @Test
    void isWithinAllowedPastWindow_shouldReturnTrue_forRecentPast() {
        assertThat(DateUtils.isWithinAllowedPastWindow(LocalDateTime.now().minusHours(1))).isTrue();
    }

    @Test
    void isWithinAllowedPastWindow_shouldReturnFalse_forTooOld() {
        assertThat(DateUtils.isWithinAllowedPastWindow(LocalDateTime.now().minusHours(25))).isFalse();
    }

    @Test
    void isWithinAllowedPastWindow_shouldReturnFalse_forNull() {
        assertThat(DateUtils.isWithinAllowedPastWindow(null)).isFalse();
    }
}
