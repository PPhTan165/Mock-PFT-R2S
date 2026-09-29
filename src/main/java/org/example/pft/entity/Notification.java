package org.example.pft.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @Column(name = "daily_reminder")
    private Boolean dailyReminder;

    @Column(name = "tip_enabled")
    private Boolean tipEnabled;

    @Column(name = "budget_alert")
    private Boolean budgetAlert;

    private LocalDateTime createdAt = LocalDateTime.now();
}
