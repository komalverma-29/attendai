package com.attendai.core.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing for the AttendAI platform.
 *
 * This configuration activates automatic population of {@code @CreatedDate},
 * {@code @LastModifiedDate}, {@code @CreatedBy}, and {@code @LastModifiedBy}
 * fields on all entities that extend {@link com.attendai.core.common.entity.BaseEntity}.
 *
 * The {@code auditorAwareRef} points to the
 * {@link com.attendai.core.common.audit.AttendAIAuditorAware} bean, which
 * resolves the current user ID from the Spring Security context.
 *
 * This class contains no other Spring configuration to keep it focused.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "attendAIAuditorAware")
public class JpaAuditingConfig {
}
