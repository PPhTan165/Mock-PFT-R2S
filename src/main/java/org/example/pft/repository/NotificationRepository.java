package org.example.pft.repository;

import org.example.pft.entity.Notification;
import org.example.pft.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    Optional<Notification> findByUser(User user);
}
