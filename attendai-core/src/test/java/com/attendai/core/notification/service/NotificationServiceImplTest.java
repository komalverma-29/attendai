package com.attendai.core.notification.service;

import com.attendai.core.notification.config.NotificationProperties;
import com.attendai.core.notification.dto.SendNotificationRequest;
import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.InAppNotification;
import com.attendai.core.notification.entity.NotificationLog;
import com.attendai.core.notification.entity.NotificationStatus;
import com.attendai.core.notification.entity.NotificationTemplate;
import com.attendai.core.notification.repository.InAppNotificationRepository;
import com.attendai.core.notification.repository.NotificationLogRepository;
import com.attendai.core.notification.repository.NotificationPreferenceRepository;
import com.attendai.core.notification.repository.NotificationTemplateRepository;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationTemplateRepository   templateRepository;
    @Mock NotificationLogRepository        logRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock InAppNotificationRepository      inAppRepository;
    @Mock EmailDispatcher                  emailDispatcher;
    @Mock PushDispatcher                   pushDispatcher;
    @Mock UserService                      userService;

    private NotificationProperties       notificationProperties;
    private NotificationServiceImpl      notificationService;
    private TemplateRenderer             templateRenderer;

    @BeforeEach
    void setUp() {
        notificationProperties = new NotificationProperties();
        templateRenderer       = new TemplateRenderer();
        notificationService    = new NotificationServiceImpl(
                templateRepository, logRepository, preferenceRepository,
                inAppRepository, templateRenderer, emailDispatcher,
                pushDispatcher, userService, notificationProperties);
    }

    // -------------------------------------------------------------------------
    // Core contract: send() must never throw
    // -------------------------------------------------------------------------

    @Test
    void send_shouldNeverThrow_evenOnInternalException() {
        when(templateRepository.findByTypeCodeAndChannelAndLocaleAndIsActiveTrue(any(), any(), any()))
                .thenThrow(new RuntimeException("DB is down"));

        SendNotificationRequest request = buildRequest(List.of("IN_APP"));

        assertThatCode(() -> notificationService.send(request))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // IN_APP dispatch
    // -------------------------------------------------------------------------

    @Test
    void send_shouldPersistInAppNotification_whenTemplateFound() {
        stubNoPreference();
        stubTemplate(Channel.IN_APP, "Inbox title", "Your request was processed.");

        when(inAppRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(buildRequest(List.of("IN_APP")));

        ArgumentCaptor<InAppNotification> captor = ArgumentCaptor.forClass(InAppNotification.class);
        verify(inAppRepository).save(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Your request was processed.");
    }

    @Test
    void send_shouldLogAsSent_forSuccessfulInApp() {
        stubNoPreference();
        stubTemplate(Channel.IN_APP, "Title", "Body");
        when(inAppRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(buildRequest(List.of("IN_APP")));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    // -------------------------------------------------------------------------
    // Opt-out preference
    // -------------------------------------------------------------------------

    @Test
    void send_shouldLogAsSkipped_whenUserOptedOut() {
        var pref = com.attendai.core.notification.entity.NotificationPreference.builder()
                .userId(1L).typeCode("TEST_TYPE").channel(Channel.IN_APP).isEnabled(false).build();
        when(preferenceRepository.findByUserIdAndTypeCodeAndChannel(1L, "TEST_TYPE", Channel.IN_APP))
                .thenReturn(Optional.of(pref));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(buildRequest(List.of("IN_APP")));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        verify(inAppRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Missing template
    // -------------------------------------------------------------------------

    @Test
    void send_shouldLogAsFailed_whenNoTemplateFound() {
        stubNoPreference();
        when(templateRepository.findByTypeCodeAndChannelAndLocaleAndIsActiveTrue(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(buildRequest(List.of("EMAIL")));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(emailDispatcher, never()).send(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // EMAIL dispatch
    // -------------------------------------------------------------------------

    @Test
    void send_shouldSendEmail_whenTemplateFoundAndEmailAvailable() {
        stubNoPreference();
        stubTemplate(Channel.EMAIL, "Reset your password", "Click {{resetLink}}.");
        when(userService.findByIdForAuth(1L))
                .thenReturn(Optional.of(
                        new UserAuthProjection(1L, "user@example.com", "hash", "ACTIVE", false)));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientUserId(1L)
                .typeCode("TEST_TYPE")
                .channels(List.of("EMAIL"))
                .variables(Map.of("resetLink", "https://example.com/reset"))
                .build();

        notificationService.send(request);

        verify(emailDispatcher).send("user@example.com",
                "Reset your password",
                "Click https://example.com/reset.");
    }

    @Test
    void send_shouldLogAsSkipped_whenNoEmailOnUserAccount() {
        stubNoPreference();
        stubTemplate(Channel.EMAIL, "Subject", "Body.");
        when(userService.findByIdForAuth(1L)).thenReturn(Optional.empty());
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(buildRequest(List.of("EMAIL")));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        verify(emailDispatcher, never()).send(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // Unknown channel
    // -------------------------------------------------------------------------

    @Test
    void send_shouldSkipUnknownChannel_withoutThrowing() {
        assertThatCode(() ->
                notificationService.send(buildRequest(List.of("SMS"))))
                .doesNotThrowAnyException();
        verify(logRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SendNotificationRequest buildRequest(List<String> channels) {
        return SendNotificationRequest.builder()
                .recipientUserId(1L)
                .typeCode("TEST_TYPE")
                .channels(channels)
                .variables(Map.of())
                .build();
    }

    private void stubNoPreference() {
        when(preferenceRepository.findByUserIdAndTypeCodeAndChannel(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private void stubTemplate(Channel channel, String subject, String body) {
        NotificationTemplate template = NotificationTemplate.builder()
                .typeCode("TEST_TYPE")
                .channel(channel)
                .locale("en")
                .subject(subject)
                .bodyTemplate(body)
                .isActive(true)
                .build();
        when(templateRepository.findByTypeCodeAndChannelAndLocaleAndIsActiveTrue(
                "TEST_TYPE", channel, "en"))
                .thenReturn(Optional.of(template));
    }
}
