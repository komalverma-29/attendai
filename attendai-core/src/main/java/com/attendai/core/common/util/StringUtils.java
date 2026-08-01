package com.attendai.core.common.util;

/**
 * String utility methods used across the AttendAI platform.
 *
 * All methods are static. This class is not instantiable.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class
    }

    /**
     * Returns {@code true} if the string is null, empty, or contains only whitespace.
     *
     * @param value the string to check
     * @return {@code true} if blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Returns {@code true} if the string is non-null and contains at least one
     * non-whitespace character.
     *
     * @param value the string to check
     * @return {@code true} if not blank
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * Trims the string and converts it to lowercase.
     * Returns an empty string if input is null.
     *
     * @param value the string to normalise
     * @return normalised string
     */
    public static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    /**
     * Trims the string and converts it to uppercase.
     * Returns an empty string if input is null.
     *
     * @param value the string to convert
     * @return uppercase string
     */
    public static String toUpperCase(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    /**
     * Truncates the string to the given maximum length.
     * If the string is shorter than or equal to maxLength, it is returned as-is.
     *
     * @param value     the string to truncate
     * @param maxLength the maximum allowed length
     * @return truncated string, or the original if already within bounds
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * Assembles a full name from parts, skipping null or blank segments.
     * Parts are joined with a single space.
     *
     * @param parts name parts (e.g., firstName, middleName, lastName)
     * @return full name string
     */
    public static String buildFullName(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (isNotBlank(part)) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }

    /**
     * Removes all path-separator characters from a filename to prevent path traversal.
     *
     * @param filename the original filename
     * @return sanitised filename
     */
    public static String sanitiseFilename(String filename) {
        if (filename == null) {
            return null;
        }
        return filename.replaceAll("[/\\\\]", "_").trim();
    }
}
