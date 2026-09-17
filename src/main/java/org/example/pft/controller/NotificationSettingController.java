package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.notification.NotificationResponse;
import org.example.pft.dto.notification.NotificationUpdateRequest;
import org.example.pft.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/settings")
@AllArgsConstructor
public class NotificationSettingController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationResponse> showNotificationSetting(){
        return ResponseEntity.ok().body(notificationService.showNotificationSetting());
    }

    @PutMapping
    public ResponseEntity<NotificationResponse> updateNotificationSetting(@RequestBody @Valid NotificationUpdateRequest request){
        return ResponseEntity.ok().body(notificationService.updateNotificationSetting(request));
    }

}
