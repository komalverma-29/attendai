package com.attendai.core.notification.repository;

import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /** Find a template for dispatch — must be active and not deleted. */
    Optional<NotificationTemplate> findByTypeCodeAndChannelAndLocaleAndIsActiveTrue(
            String typeCode, Channel channel, String locale);

    /** Fallback: find active template without locale filter (for default "en"). */
    Optional<NotificationTemplate> findByTypeCodeAndChannelAndIsActiveTrue(
            String typeCode, Channel channel);

    Page<NotificationTemplate> findAllByIsDeletedFalseOrderByTypeCodeAsc(Pageable pageable);
}
