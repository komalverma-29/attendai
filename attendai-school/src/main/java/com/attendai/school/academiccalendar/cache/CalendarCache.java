package com.attendai.school.academiccalendar.cache;

import com.attendai.school.academiccalendar.entity.SchoolCalendarEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for academic calendar entries, keyed by {@code "<schoolId>:<academicYearId>"}.
 *
 * <p>The full list of calendar entries for a (school, academic year) pair is small
 * (max ~250 entries) and is loaded once then served from memory.
 *
 * <p>Cache strategy:
 * <ul>
 *   <li>On read: cache-first, DB fallback on miss (handled by the service)</li>
 *   <li>On write: DB write → immediate cache eviction (force reload on next read)</li>
 *   <li>TTL: full cache cleared every 5 minutes by {@link #scheduledInvalidation()}</li>
 * </ul>
 *
 * <p>Eviction on write ensures that {@code isWorkingDay()} always reflects the latest
 * calendar without requiring a restart.
 */
@Slf4j
@Component
public class CalendarCache {

    /** Store: composite key → immutable list of calendar entries for that school+year. */
    private final ConcurrentHashMap<String, List<SchoolCalendarEntry>> store =
            new ConcurrentHashMap<>();

    /**
     * Returns the cached entry list for (schoolId, academicYearId), or empty on miss.
     */
    public Optional<List<SchoolCalendarEntry>> get(Long schoolId, Long academicYearId) {
        List<SchoolCalendarEntry> cached = store.get(cacheKey(schoolId, academicYearId));
        if (cached != null) {
            log.debug("CalendarCache HIT | schoolId={} yearId={}", schoolId, academicYearId);
        }
        return Optional.ofNullable(cached);
    }

    /**
     * Stores the entry list for (schoolId, academicYearId).
     * Wraps in an unmodifiable list to prevent accidental mutation.
     */
    public void put(Long schoolId, Long academicYearId, List<SchoolCalendarEntry> entries) {
        store.put(cacheKey(schoolId, academicYearId),
                  Collections.unmodifiableList(entries));
        log.debug("CalendarCache PUT | schoolId={} yearId={} entries={}",
                  schoolId, academicYearId, entries.size());
    }

    /**
     * Evicts the cached entry list for (schoolId, academicYearId).
     * Called immediately after any create/update/delete operation to force a DB reload.
     */
    public void evict(Long schoolId, Long academicYearId) {
        store.remove(cacheKey(schoolId, academicYearId));
        log.debug("CalendarCache EVICT | schoolId={} yearId={}", schoolId, academicYearId);
    }

    /** Clears the entire cache. Used in tests and by the scheduled TTL job. */
    public void invalidateAll() {
        store.clear();
        log.debug("CalendarCache fully invalidated");
    }

    /** Returns the number of cached year-school pairs. */
    public int size() {
        return store.size();
    }

    /**
     * TTL-based full cache invalidation every 5 minutes.
     * Forces a DB reload on the next read for any school+year.
     */
    @Scheduled(fixedDelay = 300_000)
    public void scheduledInvalidation() {
        if (!store.isEmpty()) {
            log.debug("CalendarCache TTL invalidation — clearing {} year-caches", store.size());
            store.clear();
        }
    }

    private String cacheKey(Long schoolId, Long academicYearId) {
        return schoolId + ":" + academicYearId;
    }
}
