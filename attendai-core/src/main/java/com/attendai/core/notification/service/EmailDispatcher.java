package com.attendai.core.notification.service;

/**
 * Abstraction over the email delivery mechanism.
 *
 * <p>Concrete implementations: {@code SmtpEmailDispatcher}, {@code SendGridEmailDispatcher}.
 * The active implementation is injected by Spring.
 *
 * <p>{@link NotificationServiceImpl} uses only this interface — swapping the
 * email provider requires only a config change, no code changes.
 */
public interface EmailDispatcher {

    /**
     * Sends an email to the given recipient.
     *
     * @param recipientEmail recipient email address
     * @param subject        email subject line
     * @param body           rendered email body (plain text or HTML)
     */
    void send(String recipientEmail, String subject, String body);
}
