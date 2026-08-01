package com.attendai.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AttendAI Core Spring Boot application entry point.
 *
 * The {@code @EnableScheduling} annotation activates scheduled jobs defined
 * in the platform (e.g., {@link com.attendai.core.auth.scheduler.TokenCleanupScheduler}).
 */
@SpringBootApplication
@EnableScheduling
public class AttendAICoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendAICoreApplication.class, args);
    }
}
