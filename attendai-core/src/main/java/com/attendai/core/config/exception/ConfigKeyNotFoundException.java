package com.attendai.core.config.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

/**
 * Thrown when a configuration key is not found in the database
 * and no default value was provided by the caller.
 */
public class ConfigKeyNotFoundException extends ResourceNotFoundException {

    public ConfigKeyNotFoundException(String key) {
        super("Configuration key '" + key + "' was not found");
    }
}
