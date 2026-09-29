package org.example.pft.service;

import org.example.pft.dto.notification.NotificationResponse;
import org.example.pft.dto.notification.NotificationUpdateRequest;
import org.example.pft.entity.Notification;
import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.NotificationRepository;
import org.example.pft.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceImplTest {

    @Mock
    CurrentUserHelper currentUserHelper;

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotificationServiceImpl notificationService;

    private User user;
    private Notification notification;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setCreatedAt(LocalDateTime.of(2026, 9, 17, 10, 0));

        notification = new Notification();
        notification.setId(10L);
        notification.setUser(user);
        notification.setDailyReminder(true);
        notification.setTipEnabled(false);
        notification.setBudgetAlert(true);
        notification.setCreatedAt(LocalDateTime.of(2026, 9, 17, 10, 5));
    }

    @Test
    void showNotificationSetting_shouldReturnCurrentUserSettings() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(notificationRepository.findByUser(user))
                .thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.showNotificationSetting();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Notification settings fetched successfully", response.getMessage());
        assertEquals(true, response.getData().getDailyReminder());
        assertEquals(false, response.getData().getTipsEnabled());
        assertEquals(true, response.getData().getBudgetAlert());

        verify(currentUserHelper).getCurrentUser();
        verify(notificationRepository).findByUser(user);
    }

    @Test
    void showNotificationSetting_whenSettingNotFound_shouldThrowResourceNotFound() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(notificationRepository.findByUser(user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.showNotificationSetting()
        );

        verify(notificationRepository).findByUser(user);
    }

    @Test
    void updateNotificationSetting_shouldSaveUpdatedSettingsAndReturnResponse() {
        NotificationUpdateRequest request = new NotificationUpdateRequest();
        request.setDailyReminder(false);
        request.setTipsEnabled(true);
        request.setBudgetAlert(false);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(notificationRepository.findByUser(user))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.updateNotificationSetting(request);

        assertTrue(response.isSuccess());
        assertEquals("Notification settings updated successfully", response.getMessage());
        assertEquals(false, response.getData().getDailyReminder());
        assertEquals(true, response.getData().getTipsEnabled());
        assertEquals(false, response.getData().getBudgetAlert());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();

        assertFalse(savedNotification.getDailyReminder());
        assertTrue(savedNotification.getTipEnabled());
        assertFalse(savedNotification.getBudgetAlert());
    }

    @Test
    void updateNotificationSetting_whenSettingNotFound_shouldThrowResourceNotFound() {
        NotificationUpdateRequest request = new NotificationUpdateRequest();
        request.setDailyReminder(false);
        request.setTipsEnabled(true);
        request.setBudgetAlert(false);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(notificationRepository.findByUser(user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.updateNotificationSetting(request)
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_shouldCreateDefaultDisabledSettings() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.createNotification(user);

        assertTrue(response.isSuccess());
        assertEquals("Notification setting created successfully", response.getMessage());
        assertFalse(response.getData().getDailyReminder());
        assertFalse(response.getData().getTipsEnabled());
        assertFalse(response.getData().getBudgetAlert());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();

        assertEquals(user, savedNotification.getUser());
        assertFalse(savedNotification.getDailyReminder());
        assertFalse(savedNotification.getTipEnabled());
        assertFalse(savedNotification.getBudgetAlert());
        assertEquals(user.getCreatedAt(), savedNotification.getCreatedAt());
    }
}
