package org.example.pft.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationUpdateRequest {
    @NotNull(message = "Daily Reminder is required")
    private Boolean dailyReminder;

    @NotNull(message = "Tip Enabled is required")
    private Boolean tipsEnabled;

    @NotNull(message = "Budget Alert is required")
    private Boolean budgetAlert;
}
