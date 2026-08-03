package com.attendai.core.config.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigCacheTest {

    private ConfigCache cache;

    @BeforeEach
    void setUp() {
        cache = new ConfigCache();
    }

    @Test
    void get_shouldReturnEmpty_onCacheMiss() {
        assertThat(cache.get("nonexistent.key")).isEmpty();
    }

    @Test
    void get_shouldReturnValue_afterPut() {
        cache.put("face.threshold", "0.85");
        assertThat(cache.get("face.threshold")).isPresent().contains("0.85");
    }

    @Test
    void evict_shouldRemoveKey() {
        cache.put("face.threshold", "0.85");
        cache.evict("face.threshold");
        assertThat(cache.get("face.threshold")).isEmpty();
    }

    @Test
    void evict_shouldNotThrow_whenKeyDoesNotExist() {
        // Must not throw
        cache.evict("nonexistent.key");
    }

    @Test
    void invalidateAll_shouldClearAllEntries() {
        cache.put("key1", "val1");
        cache.put("key2", "val2");
        cache.put("key3", "val3");

        assertThat(cache.size()).isEqualTo(3);

        cache.invalidateAll();

        assertThat(cache.size()).isZero();
        assertThat(cache.get("key1")).isEmpty();
        assertThat(cache.get("key2")).isEmpty();
    }

    @Test
    void scheduledInvalidation_shouldClearCache() {
        cache.put("some.key", "value");
        assertThat(cache.size()).isEqualTo(1);

        cache.scheduledInvalidation();

        assertThat(cache.size()).isZero();
    }

    @Test
    void scheduledInvalidation_shouldDoNothing_whenCacheIsEmpty() {
        // Must not throw on empty cache
        cache.scheduledInvalidation();
        assertThat(cache.size()).isZero();
    }

    @Test
    void put_shouldOverwriteExistingValue() {
        cache.put("face.threshold", "0.85");
        cache.put("face.threshold", "0.90");

        assertThat(cache.get("face.threshold")).contains("0.90");
    }

    @Test
    void size_shouldReflectCurrentEntryCount() {
        assertThat(cache.size()).isZero();
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertThat(cache.size()).isEqualTo(2);
        cache.evict("k1");
        assertThat(cache.size()).isEqualTo(1);
    }
}
