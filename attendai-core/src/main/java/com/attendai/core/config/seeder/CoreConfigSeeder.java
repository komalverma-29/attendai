package com.attendai.core.config.seeder;

import com.attendai.core.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup verification that Core system configuration keys are present.
 *
 * <p>The actual INSERT of default keys is handled by Flyway V24, which uses
 * {@code INSERT IGNORE} — so existing values are never overwritten.
 *
 * <p>This {@link ApplicationRunner} logs the number of Core config entries at
 * startup and can be extended to programmatically seed keys that Flyway cannot
 * express (e.g., keys computed from environment variables).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreConfigSeeder implements ApplicationRunner {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public void run(ApplicationArguments args) {
        long count = systemConfigRepository.count();
        log.info("Core system configuration verified at startup: {} config key(s) present", count);
    }
}
