package org.paykesho.notificationservice.notifications.repository;

import org.commons.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationsRepository extends JpaRepository<Notification, Integer> {
    Notification findTopByOrderByIdDesc();

    Notification findNotificationById(Integer id);
}
