package com.attendai.core.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op email dispatcher used when email is disabled or an SMTP provider
 * has not been configured.
 *
 * Active when {@code attendai.notification.email-enabled=false}.
 * In production, replace this with an {@link SmtpEmailDispatcher} or
 * provider-specific implementation.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "attendai.notification.email-enabled",
        havingValue = "false", matchIfMissing = false)
public class NoOpEmailDispatcher implements EmailDispatcher {

    @Override
    public void send(String recipientEmail, String subject, String body) {
        log.debug("NoOpEmailDispatcher: email to [REDACTED] suppressed | subject={}", subject);
    }
}
