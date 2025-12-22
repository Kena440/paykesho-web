package org.paykesho.backoffice.notification.repository;

import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for NotificationRequest entity in backoffice module
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    /**
     * Count notifications by source module
     */
    long countBySourceModule(String sourceModule);

    /**
     * Count notifications by source module and status
     */
    long countBySourceModuleAndStatus(String sourceModule, NotificationStatus status);

    /**
     * Find notifications by status with pagination
     */
    Page<NotificationRequest> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * Find notifications by notification type with pagination
     */
    Page<NotificationRequest> findByNotificationType(NotificationType notificationType, Pageable pageable);

    /**
     * Find notifications by status and notification type with pagination
     */
    Page<NotificationRequest> findByStatusAndNotificationType(NotificationStatus status, NotificationType notificationType, Pageable pageable);

    /**
     * Find notifications by recipient email
     */
    List<NotificationRequest> findByRecipientEmail(String recipientEmail);

    /**
     * Count notifications by status
     */
    long countByStatus(NotificationStatus status);
}
