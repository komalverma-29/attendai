package com.attendai.core.common.exception;

/**
 * Thrown when a requested resource cannot be found.
 * Maps to HTTP 404 Not Found.
 *
 * Usage example:
 * <pre>
 *   throw new ResourceNotFoundException("User with id " + id + " was not found");
 * </pre>
 */
public class ResourceNotFoundException extends AttendAIException {

    private static final String ERROR_CODE = "NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(ERROR_CODE, message);
    }

    /**
     * Convenience factory for the common "Entity with id X was not found" pattern.
     *
     * @param entityName the entity type (e.g., "User", "Student")
     * @param id         the ID that was not found
     */
    public static ResourceNotFoundException of(String entityName, Long id) {
        return new ResourceNotFoundException(entityName + " with id " + id + " was not found");
    }

    /**
     * Convenience factory for the common "Entity with field = value was not found" pattern.
     *
     * @param entityName the entity type
     * @param field      the field name (e.g., "email")
     * @param value      the field value
     */
    public static ResourceNotFoundException of(String entityName, String field, String value) {
        return new ResourceNotFoundException(entityName + " with " + field + " '" + value + "' was not found");
    }
}
