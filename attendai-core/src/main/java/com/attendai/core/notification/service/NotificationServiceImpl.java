package com.attendai.core.notification.service;

import com.attendai.core.notification.config.NotificationProperties;
import com.attendai.core.notification.dto.SendNotificationRequest;
import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.InAppNotification;
import com.attendai.core.notification.entity.NotificationLog;
import com.attendai.core.notification.entity.NotificationStatus;
import com.attendai.core.notification.entity.NotificationTemplate;
import com.attendai.core.notification.repository.InAppNotificationRepository;
import com.attendai.core.notification.repository.NotificationLogRepository;
import com.attendai.core.notification.repository.NotificationPreferenceRepository;
import com.attendai.core.notification.repository.NotificationTemplateRepository;
import com.attendai.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Full implementation of {@link NotificationService}.
 *
 * <p>Key contract: {@link #send} NEVER throws to the caller — all exceptions
 * are caught internally, logged, and reflected in the {@code NotificationLog}.
 *
 * <p>Dispatch flow per channel:
 * <ol>
 *   <li>Check user preference → skip if opted out</li>
 *   <li>Look up template → log FAILED if not found</li>
 *   <li>Render template with variables</li>
 *   <li>If scheduled → persist PENDING log and return</li>
 *   <li>If immediate → dispatch + persist SENT or FAILED log</li>
 *   <li>IN_APP: always persisted regardless of other channel outcomes</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationTemplateRepository   templateRepository;
    private final NotificationLogRepository        logRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final InAppNotificationRepository      inAppRepository;
    private final TemplateRenderer                 templateRenderer;
    private final EmailDispatcher                  emailDispatcher;
    private final PushDispatcher                   pushDispatcher;
    private final UserService                      userService;
    private final NotificationProperties           notificationProperties;

    @Override
    public void send(SendNotificationRequest request) {
        try {
            doSend(request);
        } catch (Exception e) {
            // Top-level safety net — should never reach here because doSend handles errors per channel.
            log.error("Unexpected error in NotificationService.send for typeCode={}: {}",
                    request.getTypeCode(), e.getMessage(), e);
        }
    }

    private void doSend(SendNotificationRequest request) {
        List<String> channelNames = request.getChannels();
        if (channelNames == null || channelNames.isEmpty()) {
            log.debug("No channels specified for typeCode={}; skipping", request.getTypeCode());
            return;
        }

        for (String channelName : channelNames) {
            Channel channel;
            try {
                channel = Channel.valueOf(channelName.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown channel '{}' for typeCode={}; skipping", channelName, request.getTypeCode());
                continue;
            }

            dispatchToChannel(request, channel);
        }
    }

    private void dispatchToChannel(SendNotificationRequest request, Channel channel) {
        // 1. Check user preference
        if (!isChannelEnabled(request.getRecipientUserId(), request.getTypeCode(), channel)) {
            persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                    NotificationStatus.SKIPPED, null, null,
                    "User opted out of channel " + channel, 0, request.getScheduledAt(), null);
            log.debug("Notification skipped (user opt-out) | userId={} typeCode={} channel={}",
                    request.getRecipientUserId(), request.getTypeCode(), channel);
            return;
        }

        // 2. Load template
        String locale = request.getLocale() != null ? request.getLocale() : "en";
        Optional<NotificationTemplate> templateOpt =
                templateRepository.findByTypeCodeAndChannelAndLocaleAndIsActiveTrue(
                        request.getTypeCode(), channel, locale);

        // Fallback to default locale if not found for specific locale
        if (templateOpt.isEmpty() && !"en".equals(locale)) {
            templateOpt = templateRepository.findByTypeCodeAndChannelAndIsActiveTrue(
                    request.getTypeCode(), channel);
        }

        if (templateOpt.isEmpty()) {
            persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                    NotificationStatus.FAILED, null, null,
                    "No template found for type=" + request.getTypeCode() + " channel=" + channel,
                    0, request.getScheduledAt(), null);
            log.warn("No notification template found | typeCode={} channel={}", request.getTypeCode(), channel);
            return;
        }

        NotificationTemplate template = templateOpt.get();
        Map<String, String> variables = request.getVariables();

        // 3. Render
        String renderedSubject = template.getSubject() != null
                ? templateRenderer.render(template.getSubject(), variables)
                : null;
        String renderedBody = templateRenderer.render(template.getBodyTemplate(), variables);

        // 4. Scheduled → persist PENDING and return
        if (request.getScheduledAt() != null && request.getScheduledAt().isAfter(LocalDateTime.now())) {
            persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                    NotificationStatus.PENDING, renderedSubject, renderedBody,
                    null, 0, request.getScheduledAt(), null);
            log.debug("Notification scheduled | typeCode={} channel={} scheduledAt={}",
                    request.getTypeCode(), channel, request.getScheduledAt());
            return;
        }

        // 5. Dispatch immediately
        dispatch(request, channel, template, renderedSubject, renderedBody);
    }

    private void dispatch(SendNotificationRequest request, Channel channel,
                          NotificationTemplate template,
                          String renderedSubject, String renderedBody) {
        try {
            switch (channel) {
                case EMAIL -> {
                    String email = resolveEmail(request.getRecipientUserId());
                    if (email == null) {
                        persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                                NotificationStatus.SKIPPED, renderedSubject, renderedBody,
                                "No email address found for user", 1, null, null);
                        return;
                    }
                    emailDispatcher.send(email, renderedSubject, renderedBody);
                    persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                            NotificationStatus.SENT, renderedSubject, renderedBody,
                            null, 1, null, LocalDateTime.now());
                    log.info("Email sent | userId={} typeCode={}", request.getRecipientUserId(), request.getTypeCode());
                }
                case IN_APP -> {
                    // IN_APP: always persisted synchronously
                    String title = renderedSubject != null ? renderedSubject : request.getTypeCode();
                    inAppRepository.save(InAppNotification.builder()
                            .recipientUserId(request.getRecipientUserId())
                            .typeCode(request.getTypeCode())
                            .title(title)
                            .body(renderedBody)
                            .build());
                    persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                            NotificationStatus.SENT, renderedSubject, renderedBody,
                            null, 1, null, LocalDateTime.now());
                    log.debug("In-app notification persisted | userId={} typeCode={}",
                            request.getRecipientUserId(), request.getTypeCode());
                }
                case PUSH -> {
                    if (!notificationProperties.isPushEnabled()) {
                        persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                                NotificationStatus.SKIPPED, renderedSubject, renderedBody,
                                "Push is disabled", 0, null, null);
                        return;
                    }
                    String title = renderedSubject != null ? renderedSubject : request.getTypeCode();
                    pushDispatcher.send(request.getRecipientUserId(), title, renderedBody);
                    persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                            NotificationStatus.SENT, renderedSubject, renderedBody,
                            null, 1, null, LocalDateTime.now());
                }
            }
        } catch (Exception e) {
            log.error("Notification dispatch failed | userId={} typeCode={} channel={}: {}",
                    request.getRecipientUserId(), request.getTypeCode(), channel, e.getMessage());
            persistLog(request.getRecipientUserId(), request.getTypeCode(), channel,
                    NotificationStatus.FAILED, renderedSubject, renderedBody,
                    e.getMessage(), 1, null, null);
        }
    }

    /**
     * Returns true when the user has not explicitly opted out of the given channel.
     * Default is enabled (no preference record = opted in).
     */
    private boolean isChannelEnabled(Long userId, String typeCode, Channel channel) {
        return preferenceRepository
                .findByUserIdAndTypeCodeAndChannel(userId, typeCode, channel)
                .map(p -> p.isEnabled())
                .orElse(true); // default = opted in
    }

    /** Resolves the recipient's email address via the UserService → PersonService chain. */
    private String resolveEmail(Long userId) {
        try {
            return userService.findByIdForAuth(userId)
                    .map(u -> u.email())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve email for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Transactional
    public void persistLog(Long recipientUserId, String typeCode, Channel channel,
                               NotificationStatus status, String subject, String renderedBody,
                               String errorMessage, int attemptCount,
                               LocalDateTime scheduledAt, LocalDateTime sentAt) {
        try {
            NotificationLog log = NotificationLog.builder()
                    .recipientUserId(recipientUserId)
                    .typeCode(typeCode)
                    .channel(channel)
                    .status(status)
                    .subject(subject)
                    .renderedBody(renderedBody)
                    .errorMessage(errorMessage)
                    .attemptCount(attemptCount)
                    .scheduledAt(scheduledAt)
                    .sentAt(sentAt)
                    .build();
            logRepository.save(log);
        } catch (Exception e) {
            // Log persistence must never propagate — swallow and log
            NotificationServiceImpl.log.error("Failed to persist notification log: {}", e.getMessage());
        }
    }
}
