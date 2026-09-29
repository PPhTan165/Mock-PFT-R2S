package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.example.pft.dto.notification.NotiData;
import org.example.pft.dto.notification.NotificationResponse;
import org.example.pft.dto.notification.NotificationUpdateRequest;
import org.example.pft.entity.Notification;
import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.NotificationRepository;
import org.example.pft.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final CurrentUserHelper currentUserHelper;
    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse showNotificationSetting() {
        User user = currentUserHelper.getCurrentUser();

        Notification noti = notificationRepository.findByUser(user)
                .orElseThrow(()->new ResourceNotFoundException("Notification setting not found with current User"));
        return new NotificationResponse(
                true,
                "Notification settings fetched successfully",
                mapToData(noti)
        );
    }

    @Transactional
    @Override
    public NotificationResponse updateNotificationSetting(NotificationUpdateRequest request) {
        User user = currentUserHelper.getCurrentUser();

        Notification notification = notificationRepository.findByUser(user)
                .orElseThrow(()-> new ResourceNotFoundException("Notification setting not found with current User"));

        notification.setDailyReminder(request.getDailyReminder());
        notification.setTipEnabled(request.getTipsEnabled());
        notification.setBudgetAlert(request.getBudgetAlert());

        Notification newNotification = notificationRepository.save(notification);
        return new NotificationResponse(
                true,
                "Notification settings updated successfully",
                mapToData(newNotification)
        );
    }

    @Transactional
    @Override
    public NotificationResponse createNotification(User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setDailyReminder(false);
        notification.setTipEnabled(false);
        notification.setBudgetAlert(false);
        notification.setCreatedAt(user.getCreatedAt());

        notificationRepository.save(notification);

        return new NotificationResponse(
                true,
                "Notification setting created successfully",
                mapToData(notification)
        );
    }

    private NotiData mapToData(Notification notification){
        NotiData data = new NotiData();
        data.setDailyReminder(notification.getDailyReminder());
        data.setBudgetAlert(notification.getBudgetAlert());
        data.setTipsEnabled(notification.getTipEnabled());

        return data;
    }



}
