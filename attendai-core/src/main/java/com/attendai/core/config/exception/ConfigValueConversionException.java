package com.attendai.core.config.exception;

import com.attendai.core.common.exception.AttendAIException;

/**
 * Thrown when a configuration value exists but cannot be converted to
 * the requested type (e.g., the value "abc" cannot be parsed as an integer).
 *
 * This is a programming error — callers must ensure the stored value
 * matches the type they request. Maps to HTTP 500.
 */
public class ConfigValueConversionException extends AttendAIException {

    public ConfigValueConversionException(String key, String value, String targetType) {
        super("CONFIG_CONVERSION_ERROR",
                "Cannot convert value '" + value + "' for key '" + key
                        + "' to type " + targetType);
    }
}
