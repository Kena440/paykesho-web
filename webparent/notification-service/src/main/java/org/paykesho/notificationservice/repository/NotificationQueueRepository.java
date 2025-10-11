package org.paykesho.notificationservice.repository;

import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationQueue entity
 */
@Repository
public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, Integer> {

    /**
     * Find notifications by status
     */
    List<NotificationQueue> findByStatus(NotificationStatus status);

    /**
     * Find notifications by status with pagination
     */
    Page<NotificationQueue> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * Find notifications by type and status
     */
    List<NotificationQueue> findByNotificationTypeAndStatus(NotificationType notificationType, NotificationStatus status);

    /**
     * Find notifications by recipient email
     */
    List<NotificationQueue> findByRecipientEmail(String recipientEmail);

    /**
     * Find notifications by recipient email with pagination
     */
    Page<NotificationQueue> findByRecipientEmail(String recipientEmail, Pageable pageable);

    /**
     * Find pending notifications ready to be processed
     */
    @Query("SELECT n FROM NotificationQueue n WHERE n.status = :status AND " +
           "(n.scheduledFor IS NULL OR n.scheduledFor <= :currentTime) " +
           "ORDER BY n.priority ASC, n.id ASC")
    List<NotificationQueue> findPendingNotifications(@Param("status") NotificationStatus status, 
                                                    @Param("currentTime") Date currentTime);

    /**
     * Find failed notifications that can be retried
     */
    @Query("SELECT n FROM NotificationQueue n WHERE n.status = :failedStatus AND " +
           "n.retryCount < n.maxRetries AND " +
           "(n.scheduledFor IS NULL OR n.scheduledFor <= :currentTime) " +
           "ORDER BY n.priority ASC, n.id ASC")
    List<NotificationQueue> findRetryableNotifications(@Param("failedStatus") NotificationStatus failedStatus,
                                                      @Param("currentTime") Date currentTime);

    /**
     * Find notifications by entity reference and type
     */
    List<NotificationQueue> findByEntityReferenceAndEntityType(String entityReference, String entityType);

    /**
     * Find notifications created within a date range
     */
    @Query("SELECT n FROM NotificationQueue n WHERE n.id BETWEEN :startId AND :endId " +
           "ORDER BY n.id DESC")
    List<NotificationQueue> findByIdBetween(@Param("startId") Integer startId, 
                                          @Param("endId") Integer endId);

    /**
     * Find notifications created within a date range
     */
    List<NotificationQueue> findByCreatedAtBetween(Date startDate, Date endDate);

    /**
     * Find notifications sent within a date range
     */
    @Query("SELECT n FROM NotificationQueue n WHERE n.sentAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.sentAt DESC")
    List<NotificationQueue> findBySentAtBetween(@Param("startDate") Date startDate, 
                                               @Param("endDate") Date endDate);

    /**
     * Count notifications by status
     */
    long countByStatus(NotificationStatus status);

    /**
     * Count notifications by type and status
     */
    long countByNotificationTypeAndStatus(NotificationType notificationType, NotificationStatus status);

    /**
     * Count notifications by recipient email and status
     */
    long countByRecipientEmailAndStatus(String recipientEmail, NotificationStatus status);

    /**
     * Find notifications that have been processing for too long (stale processing)
     */
    @Query("SELECT n FROM NotificationQueue n WHERE n.status = :processingStatus AND " +
           "n.processingStartedAt < :cutoffTime")
    List<NotificationQueue> findStaleProcessingNotifications(@Param("processingStatus") NotificationStatus processingStatus,
                                                            @Param("cutoffTime") Date cutoffTime);

    /**
     * Reset stale processing notifications back to pending
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationQueue n SET n.status = :pendingStatus, n.processingStartedAt = NULL " +
           "WHERE n.status = :processingStatus AND n.processingStartedAt < :cutoffTime")
    int resetStaleProcessingNotifications(@Param("processingStatus") NotificationStatus processingStatus,
                                         @Param("pendingStatus") NotificationStatus pendingStatus,
                                         @Param("cutoffTime") Date cutoffTime);

    /**
     * Delete old notifications (for cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQueue n WHERE n.status IN (:completedStatuses) AND " +
           "(n.sentAt < :cutoffDate OR (n.sentAt IS NULL AND n.processingStartedAt < :cutoffDate))")
    int deleteOldNotifications(@Param("completedStatuses") List<NotificationStatus> completedStatuses,
                              @Param("cutoffDate") Date cutoffDate);

    /**
     * Find notifications with specific priority and status
     */
    List<NotificationQueue> findByPriorityAndStatus(Integer priority, NotificationStatus status);

    /**
     * Find notifications by template name
     */
    List<NotificationQueue> findByTemplateName(String templateName);

    /**
     * Find notifications by template name and status
     */
    List<NotificationQueue> findByTemplateNameAndStatus(String templateName, NotificationStatus status);

    /**
     * Get notification statistics
     */
    @Query("SELECT n.status, COUNT(n) FROM NotificationQueue n GROUP BY n.status")
    List<Object[]> getNotificationStatistics();

    /**
     * Get notification statistics by type
     */
    @Query("SELECT n.notificationType, n.status, COUNT(n) FROM NotificationQueue n GROUP BY n.notificationType, n.status")
    List<Object[]> getNotificationStatisticsByType();

    // UUID-based methods for secure URL access
    Optional<NotificationQueue> findByUuid(String uuid);
    
    @Query("SELECT COUNT(n) > 0 FROM NotificationQueue n WHERE n.uuid = ?1")
    boolean existsByUuid(String uuid);
}
