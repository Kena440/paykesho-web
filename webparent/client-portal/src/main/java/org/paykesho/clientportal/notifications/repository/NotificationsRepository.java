package org.paykesho.clientportal.notifications.repository;

import org.commons.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationsRepository extends JpaRepository<Notification, Long> {
    // Additional query methods can be defined here
}
