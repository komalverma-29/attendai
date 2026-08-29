package com.attendai.core.notification.service;

import com.attendai.core.common.exception.ExternalServiceException;
import com.attendai.core.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP-based email dispatcher using Spring's {@link JavaMailSender}.
 *
 * Active when {@code attendai.notification.email-enabled=true} (the default).
 * SMTP connection properties (host, port, credentials) are configured via
 * Spring Boot's standard {@code spring.mail.*} properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "attendai.notification.email-enabled",
        havingValue = "true", matchIfMissing = true)
public class SmtpEmailDispatcher implements EmailDispatcher {

    private final JavaMailSender          mailSender;
    private final NotificationProperties  notificationProperties;

    @Override
    public void send(String recipientEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getEmailFrom());
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);

            if (notificationProperties.getEmailReplyTo() != null) {
                message.setReplyTo(notificationProperties.getEmailReplyTo());
            }

            mailSender.send(message);
            log.debug("Email sent via SMTP | subject={}", subject);
        } catch (Exception e) {
            throw new ExternalServiceException("SMTP delivery failed: " + e.getMessage(), e);
        }
    }
}
