package com.attendai.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AttendAI School Spring Boot application entry point.
 *
 * <p>This module depends on {@code attendai-core} and provides all
 * school-specific business functionality on top of the Core platform engine.
 */
@SpringBootApplication
@EnableScheduling
public class AttendAISchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendAISchoolApplication.class, args);
    }
}
