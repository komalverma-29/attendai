package com.attendai.core.notification.service;

/**
 * Abstraction over the push notification delivery mechanism.
 *
 * <p>V1 default: {@link NoOpPushDispatcher} (push is disabled out of the box).
 * Future: implement with FCM or another provider.
 */
public interface PushDispatcher {

    /**
     * Sends a push notification to the user's registered device(s).
     *
     * @param userId the recipient user's surrogate ID
     * @param title  notification title
     * @param body   notification body
     */
    void send(Long userId, String title, String body);
}
