package com.attendai.core.auth.scheduler;

import com.attendai.core.auth.repository.PasswordResetTokenRepository;
import com.attendai.core.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job that removes expired authentication tokens from the database.
 *
 * <p>Runs daily at 02:00 UTC. Removes:
 * <ul>
 *   <li>Expired refresh tokens (both revoked and non-revoked)</li>
 *   <li>Expired password reset tokens</li>
 * </ul>
 *
 * Errors are logged but never propagate — cleanup failures should not affect
 * normal platform operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository       refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;

    /**
     * Runs every day at 02:00 UTC.
     * Deletes expired refresh tokens and password reset tokens.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void cleanUpExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();

            int deletedRefresh = refreshTokenRepository.deleteExpiredBefore(now);
            int deletedReset   = resetTokenRepository.deleteExpiredBefore(now);

            log.info("Token cleanup complete — deleted {} refresh tokens, {} reset tokens",
                    deletedRefresh, deletedReset);
        } catch (Exception e) {
            log.error("Token cleanup job failed: {}", e.getMessage(), e);
        }
    }
}
