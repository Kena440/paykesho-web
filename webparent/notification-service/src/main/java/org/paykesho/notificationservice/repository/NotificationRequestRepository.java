package org.paykesho.notificationservice.repository;

import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository for NotificationRequest entity
 * Provides database access for notification requests
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Integer> {

    /**
     * Find all pending notifications ready for processing
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE nr.status = :status " +
           "AND (nr.scheduledFor IS NULL OR nr.scheduledFor <= :now) " +
           "AND nr.attemptCount < nr.maxAttempts " +
           "ORDER BY nr.priority ASC, nr.createdAt ASC")
    List<NotificationRequest> findPendingNotificationsReadyForProcessing(
        @Param("status") NotificationStatus status, 
        @Param("now") Date now
    );

    /**
     * Find notifications by status
     */
    List<NotificationRequest> findByStatus(NotificationStatus status);

    /**
     * Find notifications by status with pagination
     */
    Page<NotificationRequest> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * Find notifications by source module
     */
    List<NotificationRequest> findBySourceModule(String sourceModule);

    /**
     * Find notifications by source module and status
     */
    List<NotificationRequest> findBySourceModuleAndStatus(String sourceModule, NotificationStatus status);

    /**
     * Find notifications by recipient email
     */
    List<NotificationRequest> findByRecipientEmail(String recipientEmail);

    /**
     * Find notifications by notification type
     */
    List<NotificationRequest> findByNotificationType(NotificationType notificationType);

    /**
     * Find notifications by entity reference
     */
    List<NotificationRequest> findByEntityReference(String entityReference);

    /**
     * Find notifications by entity type
     */
    List<NotificationRequest> findByEntityType(String entityType);

    /**
     * Find notifications created within a date range
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE nr.createdAt BETWEEN :startDate AND :endDate")
    List<NotificationRequest> findNotificationsByDateRange(
        @Param("startDate") Date startDate, 
        @Param("endDate") Date endDate
    );

    /**
     * Find notifications by priority
     */
    List<NotificationRequest> findByPriority(Integer priority);

    /**
     * Find high priority notifications (priority 1-2)
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE nr.priority <= 2 AND nr.status = :status")
    List<NotificationRequest> findHighPriorityNotifications(@Param("status") NotificationStatus status);

    /**
     * Find notifications that have been stuck in processing for too long
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE nr.status = 'PROCESSING' " +
           "AND nr.lastProcessedAt < :stuckThreshold")
    List<NotificationRequest> findStuckProcessingNotifications(@Param("stuckThreshold") Date stuckThreshold);

    /**
     * Find failed notifications that can be retried
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE nr.status = 'FAILED' " +
           "AND nr.attemptCount < nr.maxAttempts")
    List<NotificationRequest> findRetryableFailedNotifications();

    /**
     * Count notifications by status
     */
    long countByStatus(NotificationStatus status);

    /**
     * Count notifications by source module
     */
    long countBySourceModule(String sourceModule);

    /**
     * Count notifications by notification type
     */
    long countByNotificationType(NotificationType notificationType);

    /**
     * Count notifications created today
     */
    @Query("SELECT COUNT(nr) FROM NotificationRequest nr WHERE DATE(nr.createdAt) = CURRENT_DATE")
    long countNotificationsCreatedToday();

    /**
     * Count notifications sent today
     */
    @Query("SELECT COUNT(nr) FROM NotificationRequest nr WHERE DATE(nr.sentAt) = CURRENT_DATE")
    long countNotificationsSentToday();

    /**
     * Count failed notifications today
     */
    @Query("SELECT COUNT(nr) FROM NotificationRequest nr WHERE DATE(nr.failedAt) = CURRENT_DATE")
    long countNotificationsFailedToday();

    /**
     * Find notifications for cleanup (old completed notifications)
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE nr.status IN ('SENT', 'FAILED_PERMANENTLY') " +
           "AND nr.createdAt < :cutoffDate")
    List<NotificationRequest> findNotificationsForCleanup(@Param("cutoffDate") Date cutoffDate);

    /**
     * Reset stuck processing notifications back to pending
     */
    @Modifying
    @Query("UPDATE NotificationRequest nr SET nr.status = 'PENDING', nr.lastProcessedAt = NULL " +
           "WHERE nr.status = 'PROCESSING' AND nr.lastProcessedAt < :stuckThreshold")
    int resetStuckProcessingNotifications(@Param("stuckThreshold") Date stuckThreshold);

    /**
     * Delete old completed notifications
     */
    @Modifying
    @Query("DELETE FROM NotificationRequest nr WHERE nr.status IN ('SENT', 'FAILED_PERMANENTLY') " +
           "AND nr.createdAt < :cutoffDate")
    int deleteOldCompletedNotifications(@Param("cutoffDate") Date cutoffDate);

    /**
     * Get notification statistics by status
     */
    @Query("SELECT nr.status, COUNT(nr) FROM NotificationRequest nr GROUP BY nr.status")
    List<Object[]> getNotificationStatisticsByStatus();

    /**
     * Get notification statistics by source module
     */
    @Query("SELECT nr.sourceModule, COUNT(nr) FROM NotificationRequest nr GROUP BY nr.sourceModule")
    List<Object[]> getNotificationStatisticsBySourceModule();

    /**
     * Get notification statistics by type
     */
    @Query("SELECT nr.notificationType, COUNT(nr) FROM NotificationRequest nr GROUP BY nr.notificationType")
    List<Object[]> getNotificationStatisticsByType();

    /**
     * Find notifications by multiple criteria
     */
    @Query("SELECT nr FROM NotificationRequest nr WHERE " +
           "(:status IS NULL OR nr.status = :status) AND " +
           "(:sourceModule IS NULL OR nr.sourceModule = :sourceModule) AND " +
           "(:notificationType IS NULL OR nr.notificationType = :notificationType) AND " +
           "(:recipientEmail IS NULL OR nr.recipientEmail = :recipientEmail)")
    List<NotificationRequest> findNotificationsByCriteria(
        @Param("status") NotificationStatus status,
        @Param("sourceModule") String sourceModule,
        @Param("notificationType") NotificationType notificationType,
        @Param("recipientEmail") String recipientEmail
    );
}
