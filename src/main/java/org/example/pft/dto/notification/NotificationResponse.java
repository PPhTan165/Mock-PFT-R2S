package org.example.pft.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationResponse {
    private boolean success;
    private String message;
    private NotiData data;
}
