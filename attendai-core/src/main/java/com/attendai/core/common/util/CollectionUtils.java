package com.attendai.core.common.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Collection utility methods used across the AttendAI platform.
 *
 * All methods are static. This class is not instantiable.
 */
public final class CollectionUtils {

    private CollectionUtils() {
        // Utility class
    }

    /**
     * Returns {@code true} if the collection is null or contains no elements.
     *
     * @param collection the collection to check
     * @return {@code true} if empty or null
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns {@code true} if the collection is non-null and contains at least one element.
     *
     * @param collection the collection to check
     * @return {@code true} if not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Returns {@code true} if the map is null or contains no entries.
     *
     * @param map the map to check
     * @return {@code true} if empty or null
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Returns the collection if non-null and non-empty, otherwise returns the provided default.
     *
     * @param collection the collection to check
     * @param defaultValue the default to return when collection is empty/null
     * @param <T> the element type
     * @return the collection or the default
     */
    public static <T> Collection<T> defaultIfEmpty(Collection<T> collection, Collection<T> defaultValue) {
        return isEmpty(collection) ? defaultValue : collection;
    }

    /**
     * Returns an unmodifiable empty list if the input list is null.
     * Useful for null-safe returns from service methods.
     *
     * @param list the list to check
     * @param <T> the element type
     * @return the list, or an empty list if null
     */
    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
