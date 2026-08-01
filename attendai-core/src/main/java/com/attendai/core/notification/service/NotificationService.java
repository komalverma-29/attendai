package com.attendai.core.notification.service;

import com.attendai.core.notification.dto.SendNotificationRequest;

/**
 * Platform notification dispatch service.
 *
 * <p>This is a fire-and-forget service. {@link #send(SendNotificationRequest)}
 * NEVER throws to the caller. Failures are caught internally, logged, and
 * retried asynchronously.
 *
 * <p>Usage:
 * <pre>{@code
 * notificationService.send(SendNotificationRequest.builder()
 *     .recipientUserId(userId)
 *     .typeCode("AUTH_PASSWORD_RESET")
 *     .channels(List.of("EMAIL"))
 *     .variables(Map.of("firstName", "John", "resetLink", "https://..."))
 *     .build());
 * }</pre>
 */
public interface NotificationService {

    /**
     * Dispatches a notification to the specified user via the requested channels.
     *
     * <p>This method never throws. All delivery failures are handled internally.
     *
     * @param request the notification dispatch request
     */
    void send(SendNotificationRequest request);
}
