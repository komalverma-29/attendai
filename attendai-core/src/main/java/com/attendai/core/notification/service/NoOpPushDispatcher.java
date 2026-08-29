package com.attendai.core.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * No-op push dispatcher. Push notifications are out of scope for V1.
 * This stub logs the call at DEBUG level and discards it.
 */
@Slf4j
@Component
public class NoOpPushDispatcher implements PushDispatcher {

    @Override
    public void send(Long userId, String title, String body) {
        log.debug("NoOpPushDispatcher: push to userId={} suppressed | title={}", userId, title);
    }
}
