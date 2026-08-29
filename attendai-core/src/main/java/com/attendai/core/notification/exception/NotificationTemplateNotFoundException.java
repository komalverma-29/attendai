package com.attendai.core.notification.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class NotificationTemplateNotFoundException extends ResourceNotFoundException {
    public NotificationTemplateNotFoundException(Long id) {
        super("Notification template with id " + id + " was not found");
    }
}
