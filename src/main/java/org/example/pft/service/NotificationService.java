package org.example.pft.service;

import org.example.pft.dto.notification.NotificationResponse;
import org.example.pft.dto.notification.NotificationUpdateRequest;
import org.example.pft.entity.User;

public interface NotificationService {
    NotificationResponse showNotificationSetting();
    NotificationResponse updateNotificationSetting(NotificationUpdateRequest request);
    NotificationResponse createNotification(User user);
}
