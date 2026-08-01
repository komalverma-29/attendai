package com.attendai.core.common.audit;

import com.attendai.core.common.security.SecurityContextUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring Data JPA {@link AuditorAware} implementation that resolves the currently
 * authenticated user's ID from the Spring Security context.
 *
 * Returns {@link Optional#empty()} for unauthenticated requests (e.g., system
 * startup, scheduled jobs, or public endpoints), so those operations do not
 * fail when no principal is present.
 *
 * This bean must be registered before {@code @EnableJpaAuditing} takes effect.
 * See {@link com.attendai.core.common.config.JpaAuditingConfig}.
 */
@Component
public class AttendAIAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        return SecurityContextUtils.getCurrentUserId();
    }
}
