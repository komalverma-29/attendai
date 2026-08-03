package com.attendai.core.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory configuration cache.
 *
 * <p>Provides sub-millisecond reads for configuration values that are read
 * on every attendance event, face recognition query, or notification dispatch.
 *
 * <p>Cache strategy:
 * <ul>
 *   <li>On read: cache-first (DB fallback on miss)</li>
 *   <li>On write: DB write → immediate cache update</li>
 *   <li>On delete: DB delete → immediate cache eviction</li>
 *   <li>TTL: full cache cleared every 60 seconds by a scheduled job,
 *       forcing a reload from DB on the next read of any key</li>
 * </ul>
 *
 * <p>This is intentionally simple. A distributed cache (Redis) is out of scope for V1.
 */
@Slf4j
@Component
public class ConfigCache {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    /**
     * Returns the cached value for a key, or {@link Optional#empty()} on a cache miss.
     *
     * @param key the configuration key (lowercase)
     * @return cached value if present
     */
    public Optional<String> get(String key) {
        String value = store.get(key);
        if (value != null) {
            log.debug("Cache hit for key: {}", key);
        } else {
            log.debug("Cache miss for key: {}", key);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Stores or updates a key-value pair in the cache.
     *
     * @param key   the configuration key (lowercase)
     * @param value the string value to cache
     */
    public void put(String key, String value) {
        store.put(key, value);
    }

    /**
     * Removes a key from the cache.
     *
     * @param key the configuration key to evict
     */
    public void evict(String key) {
        store.remove(key);
    }

    /**
     * Clears the entire cache.
     * Called by the TTL scheduler and can be used in tests.
     */
    public void invalidateAll() {
        store.clear();
        log.debug("Config cache fully invalidated");
    }

    /**
     * Returns the current number of entries in the cache.
     */
    public int size() {
        return store.size();
    }

    // -------------------------------------------------------------------------
    // TTL-based full invalidation — runs every 60 seconds
    // -------------------------------------------------------------------------

    /**
     * Clears the entire cache every 60 seconds.
     *
     * This forces a reload from the database on the next read of any key,
     * ensuring the cache never serves stale data for longer than one minute
     * even if a write bypasses the normal invalidation path.
     *
     * The 60-second interval is intentionally conservative and can be
     * adjusted via {@code @Scheduled} if needed in future.
     */
    @Scheduled(fixedDelay = 60_000)
    public void scheduledInvalidation() {
        if (!store.isEmpty()) {
            log.debug("Config cache TTL invalidation — clearing {} entries", store.size());
            store.clear();
        }
    }
}
