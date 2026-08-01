package com.attendai.core.common.exception;

/**
 * Thrown when an attempt is made to create a resource that already exists.
 * Maps to HTTP 409 Conflict.
 *
 * Usage example:
 * <pre>
 *   throw new ResourceAlreadyExistsException("User with email '" + email + "' already exists");
 * </pre>
 */
public class ResourceAlreadyExistsException extends AttendAIException {

    private static final String ERROR_CODE = "ALREADY_EXISTS";

    public ResourceAlreadyExistsException(String message) {
        super(ERROR_CODE, message);
    }

    /**
     * Convenience factory for the common "Entity with field = value already exists" pattern.
     *
     * @param entityName the entity type
     * @param field      the unique field name (e.g., "email")
     * @param value      the duplicate value
     */
    public static ResourceAlreadyExistsException of(String entityName, String field, String value) {
        return new ResourceAlreadyExistsException(
                entityName + " with " + field + " '" + value + "' already exists");
    }
}
