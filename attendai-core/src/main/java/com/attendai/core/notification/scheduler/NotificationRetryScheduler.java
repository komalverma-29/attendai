package com.attendai.core.notification.scheduler;

import com.attendai.core.notification.config.NotificationProperties;
import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.NotificationLog;
import com.attendai.core.notification.entity.NotificationStatus;
import com.attendai.core.notification.repository.NotificationLogRepository;
import com.attendai.core.notification.service.EmailDispatcher;
import com.attendai.core.notification.service.PushDispatcher;
import com.attendai.core.notification.service.NotificationServiceImpl;
import com.attendai.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that retries FAILED notification logs.
 *
 * <p>Runs every {@code attendai.notification.retry-interval-seconds} (default: 5 min).
 * After {@code attendai.notification.retry-max-attempts} failures, the log is
 * set to {@code PERMANENTLY_FAILED}.
 *
 * <p>Also processes PENDING scheduled notifications whose scheduled time has passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private final NotificationLogRepository logRepository;
    private final EmailDispatcher           emailDispatcher;
    private final PushDispatcher            pushDispatcher;
    private final NotificationProperties    notificationProperties;
    private final UserService               userService;
    private final NotificationServiceImpl   notificationServiceImpl;

    /** Runs every 5 minutes (configurable via cron). */
    @Scheduled(fixedDelayString = "${attendai.notification.retry-interval-seconds:300}000")
    @Transactional
    public void retryFailed() {
        try {
            List<NotificationLog> failed = logRepository
                    .findByStatusAndAttemptCountLessThan(
                            NotificationStatus.FAILED,
                            notificationProperties.getRetryMaxAttempts());

            for (NotificationLog logEntry : failed) {
                retryLog(logEntry);
            }

            // Also dispatch any scheduled notifications whose time has passed
            List<NotificationLog> pending = logRepository
                    .findByStatusAndScheduledAtLessThanEqual(
                            NotificationStatus.PENDING, LocalDateTime.now());

            for (NotificationLog logEntry : pending) {
                retryLog(logEntry);
            }

        } catch (Exception e) {
            log.error("Notification retry scheduler error: {}", e.getMessage(), e);
        }
    }

    private void retryLog(NotificationLog logEntry) {
        int newAttemptCount = logEntry.getAttemptCount() + 1;
        logEntry.setAttemptCount(newAttemptCount);
        logEntry.setUpdatedAt(LocalDateTime.now());

        try {
            dispatchLog(logEntry);
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
            log.info("Notification retry succeeded | logId={} typeCode={} channel={}",
                    logEntry.getId(), logEntry.getTypeCode(), logEntry.getChannel());
        } catch (Exception e) {
            if (newAttemptCount >= notificationProperties.getRetryMaxAttempts()) {
                logEntry.setStatus(NotificationStatus.PERMANENTLY_FAILED);
                logEntry.setErrorMessage(e.getMessage());
                log.warn("Notification permanently failed after {} attempts | logId={} typeCode={}",
                        newAttemptCount, logEntry.getId(), logEntry.getTypeCode());
            } else {
                logEntry.setStatus(NotificationStatus.FAILED);
                logEntry.setErrorMessage(e.getMessage());
                log.warn("Notification retry failed (attempt {}/{}) | logId={} typeCode={}",
                        newAttemptCount, notificationProperties.getRetryMaxAttempts(),
                        logEntry.getId(), logEntry.getTypeCode());
            }
        }

        logRepository.save(logEntry);
    }

    private void dispatchLog(NotificationLog logEntry) {
        switch (logEntry.getChannel()) {
            case EMAIL -> {
                String email = resolveEmail(logEntry.getRecipientUserId());
                if (email == null) {
                    throw new IllegalStateException("No email for userId=" + logEntry.getRecipientUserId());
                }
                emailDispatcher.send(email, logEntry.getSubject(), logEntry.getRenderedBody());
            }
            case IN_APP -> {
                // IN_APP logs are created synchronously — no retry needed in practice,
                // but if one ends up here it means the DB write failed and should be re-attempted.
                notificationServiceImpl.persistLog(
                        logEntry.getRecipientUserId(), logEntry.getTypeCode(),
                        Channel.IN_APP, NotificationStatus.SENT,
                        logEntry.getSubject(), logEntry.getRenderedBody(),
                        null, logEntry.getAttemptCount(), null, LocalDateTime.now());
            }
            case PUSH -> {
                if (!notificationProperties.isPushEnabled()) {
                    throw new IllegalStateException("Push is disabled");
                }
                String title = logEntry.getSubject() != null
                        ? logEntry.getSubject()
                        : logEntry.getTypeCode();
                pushDispatcher.send(logEntry.getRecipientUserId(), title, logEntry.getRenderedBody());
            }
        }
    }

    private String resolveEmail(Long userId) {
        try {
            return userService.findByIdForAuth(userId)
                    .map(u -> u.email())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
