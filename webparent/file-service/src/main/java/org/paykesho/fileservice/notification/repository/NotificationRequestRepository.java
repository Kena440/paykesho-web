package org.paykesho.fileservice.notification.repository;

import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository for NotificationRequest entities in file-service module
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {
    
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
     * Find notifications by status
     */
    List<NotificationRequest> findByStatus(NotificationStatus status);
    
    /**
     * Find pending notifications ready to be processed
     */
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = :status AND (n.scheduledFor IS NULL OR n.scheduledFor <= :now)")
    List<NotificationRequest> findPendingNotifications(@Param("status") NotificationStatus status, @Param("now") Date now);
    
    /**
     * Find retryable notifications (failed but not permanently failed)
     */
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = :status AND n.attemptCount < n.maxAttempts")
    List<NotificationRequest> findRetryableNotifications(@Param("status") NotificationStatus status);
    
    /**
     * Count notifications by source module
     */
    long countBySourceModule(String sourceModule);
    
    /**
     * Count notifications by source module and status
     */
    long countBySourceModuleAndStatus(String sourceModule, NotificationStatus status);
    
    /**
     * Count notifications by status
     */
    long countByStatus(NotificationStatus status);
    
    /**
     * Get notification statistics grouped by status
     */
    @Query("SELECT n.status, COUNT(n) FROM NotificationRequest n WHERE n.sourceModule = :sourceModule GROUP BY n.status")
    List<Object[]> getNotificationStatisticsByModule(@Param("sourceModule") String sourceModule);
    
    /**
     * Reset stale processing notifications
     */
    @Query("UPDATE NotificationRequest n SET n.status = :newStatus WHERE n.status = :oldStatus AND n.lastProcessedAt < :cutoffTime")
    int resetStaleProcessingNotifications(@Param("oldStatus") NotificationStatus oldStatus, 
                                        @Param("newStatus") NotificationStatus newStatus, 
                                        @Param("cutoffTime") Date cutoffTime);
    
    /**
     * Delete old notifications
     */
    @Query("DELETE FROM NotificationRequest n WHERE n.status IN :statuses AND n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("statuses") List<NotificationStatus> statuses, @Param("cutoffDate") Date cutoffDate);
}
