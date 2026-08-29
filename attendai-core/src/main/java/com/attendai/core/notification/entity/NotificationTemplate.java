package com.attendai.core.notification.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A named notification template for a specific type code + channel + locale combination.
 * The body uses Mustache-style {{variable}} placeholders.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate extends SoftDeletableEntity {

    @Column(name = "type_code", nullable = false, length = 100)
    private String typeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = "en";

    /** Email subject. Required for EMAIL channel, nullable for IN_APP and PUSH. */
    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
