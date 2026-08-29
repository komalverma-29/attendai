package com.attendai.core.notification.controller;

import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.notification.dto.InAppNotificationResponse;
import com.attendai.core.notification.dto.NotificationLogResponse;
import com.attendai.core.notification.dto.NotificationPreferenceRequest;
import com.attendai.core.notification.dto.NotificationPreferenceResponse;
import com.attendai.core.notification.dto.NotificationTemplateRequest;
import com.attendai.core.notification.dto.NotificationTemplateResponse;
import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.NotificationPreference;
import com.attendai.core.notification.entity.NotificationStatus;
import com.attendai.core.notification.entity.NotificationTemplate;
import com.attendai.core.notification.exception.NotificationTemplateNotFoundException;
import com.attendai.core.notification.mapper.NotificationMapper;
import com.attendai.core.notification.repository.InAppNotificationRepository;
import com.attendai.core.notification.repository.NotificationLogRepository;
import com.attendai.core.notification.repository.NotificationPreferenceRepository;
import com.attendai.core.notification.repository.NotificationTemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for notification management.
 * Base path: /api/v1/core/notifications
 */
@RestController
@RequestMapping("/api/v1/core/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationRepository      inAppRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateRepository   templateRepository;
    private final NotificationLogRepository        logRepository;
    private final NotificationMapper               notificationMapper;

    // -------------------------------------------------------------------------
    // In-App Inbox
    // -------------------------------------------------------------------------

    @GetMapping("/inbox")
    public ResponseEntity<PageResponse<InAppNotificationResponse>> getInbox(
            @RequestParam(name = "read", required = false) Boolean read,
            @Valid PageRequestParams pageParams) {

        Long userId = requireCurrentUserId();

        return ResponseEntity.ok(PageResponse.of(
                inAppRepository.findInbox(userId, read, pageParams.toPageable())
                        .map(notificationMapper::toInAppResponse)));
    }

    @PatchMapping("/inbox/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, String>>> markRead(
            @PathVariable("id") Long id) {

        Long userId = requireCurrentUserId();

        var notification = inAppRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "In-app notification with id " + id + " was not found"));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new com.attendai.core.common.exception.ForbiddenException(
                    "You can only mark your own notifications as read");
        }

        notification.markRead();
        inAppRepository.save(notification);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Notification marked as read")));
    }

    @PatchMapping("/inbox/read-all")
    public ResponseEntity<ApiResponse<Map<String, String>>> markAllRead() {
        Long userId = requireCurrentUserId();
        int updated = inAppRepository.markAllReadByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "All notifications marked as read",
                        "updatedCount", String.valueOf(updated))));
    }

    // -------------------------------------------------------------------------
    // Preferences
    // -------------------------------------------------------------------------

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponse>>> getPreferences() {
        Long userId = requireCurrentUserId();
        List<NotificationPreferenceResponse> prefs = preferenceRepository.findByUserId(userId)
                .stream()
                .map(notificationMapper::toPreferenceResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, String>>> updatePreference(
            @Valid @RequestBody NotificationPreferenceRequest request) {

        Long userId = requireCurrentUserId();

        NotificationPreference preference =
                preferenceRepository.findByUserIdAndTypeCodeAndChannel(
                        userId, request.getTypeCode(),
                        Channel.valueOf(request.getChannel().name()))
                        .orElseGet(() -> NotificationPreference.builder()
                                .userId(userId)
                                .typeCode(request.getTypeCode())
                                .channel(request.getChannel())
                                .build());

        preference.setEnabled(request.getIsEnabled());
        preference.setUpdatedAt(LocalDateTime.now());
        preferenceRepository.save(preference);

        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Preference updated")));
    }

    // -------------------------------------------------------------------------
    // Templates (admin)
    // -------------------------------------------------------------------------

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('CORE_NOTIFICATION_MANAGE')")
    public ResponseEntity<PageResponse<NotificationTemplateResponse>> listTemplates(
            @Valid PageRequestParams pageParams) {

        return ResponseEntity.ok(PageResponse.of(
                templateRepository.findAllByIsDeletedFalseOrderByTypeCodeAsc(pageParams.toPageable())
                        .map(notificationMapper::toTemplateResponse)));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('CORE_NOTIFICATION_MANAGE')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {

        NotificationTemplate template = NotificationTemplate.builder()
                .typeCode(request.getTypeCode())
                .channel(request.getChannel())
                .locale(request.getLocale() != null ? request.getLocale() : "en")
                .subject(request.getSubject())
                .bodyTemplate(request.getBodyTemplate())
                .isActive(true)
                .build();

        NotificationTemplate saved = templateRepository.save(template);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(notificationMapper.toTemplateResponse(saved)));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('CORE_NOTIFICATION_MANAGE')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplate(
            @PathVariable("id") Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {

        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new NotificationTemplateNotFoundException(id));

        if (request.getSubject()      != null) template.setSubject(request.getSubject());
        if (request.getBodyTemplate() != null) template.setBodyTemplate(request.getBodyTemplate());

        NotificationTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(ApiResponse.success(notificationMapper.toTemplateResponse(saved)));
    }

    // -------------------------------------------------------------------------
    // Delivery logs (admin)
    // -------------------------------------------------------------------------

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('CORE_NOTIFICATION_MANAGE')")
    public ResponseEntity<PageResponse<NotificationLogResponse>> getLogs(
            @RequestParam(name = "userId",   required = false) Long userId,
            @RequestParam(name = "typeCode", required = false) String typeCode,
            @RequestParam(name = "channel",  required = false) Channel channel,
            @RequestParam(name = "status",   required = false) NotificationStatus status,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "toDate",   required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Valid PageRequestParams pageParams) {

        return ResponseEntity.ok(PageResponse.of(
                logRepository.findByFilters(userId, typeCode, channel, status,
                        fromDate, toDate, pageParams.toPageable())
                        .map(notificationMapper::toLogResponse)));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Long requireCurrentUserId() {
        return SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }
}
