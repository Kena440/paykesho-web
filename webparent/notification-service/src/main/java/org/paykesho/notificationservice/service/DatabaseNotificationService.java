package org.paykesho.notificationservice.service;

import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for database-driven notifications
 * This interface is implemented by the notification-service module
 */
public interface DatabaseNotificationService {

    /**
     * Create a basic notification request
     */
    NotificationRequest createNotificationRequest(NotificationType notificationType, 
                                                String recipientEmail, 
                                                String subject, 
                                                String content, 
                                                String sourceModule);

    /**
     * Create a high priority notification request
     */
    NotificationRequest createHighPriorityNotificationRequest(NotificationType notificationType, 
                                                            String recipientEmail, 
                                                            String subject, 
                                                            String content, 
                                                            String sourceModule);

    /**
     * Create a scheduled notification request
     */
    NotificationRequest createScheduledNotificationRequest(NotificationType notificationType, 
                                                         String recipientEmail, 
                                                         String subject, 
                                                         String content, 
                                                         String sourceModule, 
                                                         Date scheduledFor);

    /**
     * Create notification request with metadata
     */
    NotificationRequest createNotificationRequestWithMetadata(NotificationType notificationType, 
                                                             String recipientEmail, 
                                                             String subject, 
                                                             String content, 
                                                             String sourceModule, 
                                                             String metadata, 
                                                             String entityReference, 
                                                             String entityType);

    /**
     * Create SMS notification request
     */
    NotificationRequest createSmsNotificationRequest(String recipientPhone, 
                                                    String content, 
                                                    String sourceModule);

    /**
     * Create password reset notification request
     */
    NotificationRequest createPasswordResetNotificationRequest(String recipientEmail, 
                                                             String userName, 
                                                             String resetUrl, 
                                                             String sourceModule);

    /**
     * Create multi-channel notification request (email + SMS)
     */
    List<NotificationRequest> createMultiChannelNotificationRequest(String recipientEmail, 
                                                                    String recipientPhone, 
                                                                    String subject, 
                                                                    String content, 
                                                                    String sourceModule);

    /**
     * Get notification request by ID
     */
    Optional<NotificationRequest> getNotificationRequest(Integer id);

    /**
     * Get notifications by status
     */
    List<NotificationRequest> getNotificationsByStatus(NotificationStatus status);

    /**
     * Get notifications by source module
     */
    List<NotificationRequest> getNotificationsBySourceModule(String sourceModule);

    /**
     * Get notifications by recipient
     */
    List<NotificationRequest> getNotificationsByRecipient(String recipientEmail);

    /**
     * Get pending notifications ready for processing
     */
    List<NotificationRequest> getPendingNotificationsReadyForProcessing();

    /**
     * Get notification statistics
     */
    Map<String, Object> getNotificationStatistics();

    /**
     * Update notification request status
     */
    void updateNotificationStatus(Integer id, NotificationStatus status);

    /**
     * Mark notification as failed
     */
    void markNotificationAsFailed(Integer id, String errorMessage);

    /**
     * Mark notification as sent
     */
    void markNotificationAsSent(Integer id);
}
