package com.attendai.core.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionUtilsTest {

    @Test
    void isEmpty_shouldReturnTrue_forNullCollection() {
        assertThat(CollectionUtils.isEmpty((List<?>) null)).isTrue();
    }

    @Test
    void isEmpty_shouldReturnTrue_forEmptyCollection() {
        assertThat(CollectionUtils.isEmpty(List.of())).isTrue();
    }

    @Test
    void isEmpty_shouldReturnFalse_forNonEmptyCollection() {
        assertThat(CollectionUtils.isEmpty(List.of("a"))).isFalse();
    }

    @Test
    void isNotEmpty_shouldReturnTrue_forNonEmptyCollection() {
        assertThat(CollectionUtils.isNotEmpty(List.of("a"))).isTrue();
    }

    @Test
    void isEmpty_shouldReturnTrue_forNullMap() {
        assertThat(CollectionUtils.isEmpty((Map<?, ?>) null)).isTrue();
    }

    @Test
    void isEmpty_shouldReturnFalse_forNonEmptyMap() {
        assertThat(CollectionUtils.isEmpty(Map.of("k", "v"))).isFalse();
    }

    @Test
    void defaultIfEmpty_shouldReturnDefault_whenCollectionIsNull() {
        List<String> result = (List<String>) CollectionUtils.defaultIfEmpty(null, List.of("default"));
        assertThat(result).containsExactly("default");
    }

    @Test
    void defaultIfEmpty_shouldReturnCollection_whenNonEmpty() {
        List<String> input = List.of("a", "b");
        List<String> result = (List<String>) CollectionUtils.defaultIfEmpty(input, List.of("default"));
        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void emptyIfNull_shouldReturnEmptyList_forNull() {
        assertThat(CollectionUtils.emptyIfNull(null)).isEmpty();
    }

    @Test
    void emptyIfNull_shouldReturnOriginalList_forNonNull() {
        List<String> input = List.of("x");
        assertThat(CollectionUtils.emptyIfNull(input)).isSameAs(input);
    }
}
