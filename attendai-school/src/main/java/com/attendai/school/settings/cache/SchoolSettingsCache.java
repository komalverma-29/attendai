package com.attendai.school.settings.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for school-level settings.
 *
 * <p>Cache key format: {@code "<schoolId>:<settingKey>"}.
 * This scopes every entry to a specific school, preventing cross-school reads.
 *
 * <p>Cache strategy (mirrors {@code ConfigCache} in core-config):
 * <ul>
 *   <li>On read: cache-first, DB fallback on miss</li>
 *   <li>On write: DB write → immediate cache update</li>
 *   <li>On delete: DB delete → immediate cache eviction</li>
 *   <li>TTL: full cache cleared every 5 minutes by a scheduled job</li>
 * </ul>
 */
@Slf4j
@Component
public class SchoolSettingsCache {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    /**
     * Returns the cached value for (schoolId, key), or empty on miss.
     */
    public Optional<String> get(Long schoolId, String key) {
        String cached = store.get(cacheKey(schoolId, key));
        if (cached != null) {
            log.debug("SchoolSettingsCache hit | schoolId={} key={}", schoolId, key);
        }
        return Optional.ofNullable(cached);
    }

    /**
     * Stores a value in the cache for (schoolId, key).
     */
    public void put(Long schoolId, String key, String value) {
        store.put(cacheKey(schoolId, key), value);
    }

    /**
     * Evicts the entry for (schoolId, key) from the cache.
     * Called immediately on write or delete to maintain consistency.
     */
    public void evict(Long schoolId, String key) {
        store.remove(cacheKey(schoolId, key));
    }

    /** Clears the entire cache (for testing). */
    public void invalidateAll() {
        store.clear();
        log.debug("SchoolSettingsCache fully invalidated");
    }

    /** Returns the current number of cached entries (for testing). */
    public int size() {
        return store.size();
    }

    /**
     * TTL-based full invalidation every 5 minutes.
     * Forces a reload from DB on the next read of any key.
     */
    @Scheduled(fixedDelay = 300_000)
    public void scheduledInvalidation() {
        if (!store.isEmpty()) {
            log.debug("SchoolSettingsCache TTL invalidation — clearing {} entries", store.size());
            store.clear();
        }
    }

    private String cacheKey(Long schoolId, String key) {
        return schoolId + ":" + key;
    }
}
