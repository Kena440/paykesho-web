package org.paykesho.notificationservice.service;

import org.commons.entity.Notification;
import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.commons.exception.ValidationException;
import org.commons.exception.ResourceNotFoundException;
import org.commons.exception.BusinessLogicException;
import org.paykesho.notificationservice.repository.NotificationQueueRepository;
import org.paykesho.notificationservice.notifications.repository.NotificationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing notification queue operations with integration to existing notification system
 */
@Service
@Transactional
public class NotificationQueueService {

    @Autowired
    private NotificationQueueRepository notificationQueueRepository;
    
    @Autowired
    private NotificationsRepository notificationsRepository;
    
    // Hardcoded configuration values
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_TEMPLATE_NAME_LENGTH = 100;

    /**
     * Add a new notification to the queue (integrates with existing Notification entity)
     */
    public NotificationQueue addNotification(NotificationType notificationType, String recipientEmail, 
                                           String subject, String content) {
        // Validate input parameters
        validateNotificationInput(notificationType, recipientEmail, subject, content);
        
        // Create queue notification
        NotificationQueue queueNotification = new NotificationQueue(notificationType, recipientEmail, subject, content);
        queueNotification = notificationQueueRepository.save(queueNotification);
        
        // Also create legacy notification for backward compatibility
        createLegacyNotification(queueNotification);
        
        return queueNotification;
    }

    /**
     * Add a template-based notification to the queue
     */
    public NotificationQueue addTemplateNotification(NotificationType notificationType, String recipientEmail, 
                                                   String templateName, String templateVariables) {
        // Validate input parameters
        validateTemplateNotificationInput(notificationType, recipientEmail, templateName);
        
        NotificationQueue notification = new NotificationQueue(notificationType, recipientEmail, templateName, templateVariables, true);
        notification = notificationQueueRepository.save(notification);
        
        // Create legacy notification
        createLegacyNotification(notification);
        
        return notification;
    }

    /**
     * Add a notification with additional metadata
     */
    public NotificationQueue addNotificationWithMetadata(NotificationType notificationType, String recipientEmail, 
                                                        String subject, String content, String metadata, 
                                                        String entityReference, String entityType) {
        NotificationQueue notification = new NotificationQueue(notificationType, recipientEmail, subject, content);
        notification.setMetadata(metadata);
        notification.setEntityReference(entityReference);
        notification.setEntityType(entityType);
        notification = notificationQueueRepository.save(notification);
        
        // Create legacy notification
        createLegacyNotification(notification);
        
        return notification;
    }

    /**
     * Add a scheduled notification
     */
    public NotificationQueue addScheduledNotification(NotificationType notificationType, String recipientEmail, 
                                                    String subject, String content, Date scheduledFor) {
        NotificationQueue notification = new NotificationQueue(notificationType, recipientEmail, subject, content);
        notification.setScheduledFor(scheduledFor);
        notification = notificationQueueRepository.save(notification);
        
        // Create legacy notification
        createLegacyNotification(notification);
        
        return notification;
    }

    /**
     * Add a high priority notification
     */
    public NotificationQueue addHighPriorityNotification(NotificationType notificationType, String recipientEmail, 
                                                        String subject, String content) {
        NotificationQueue notification = new NotificationQueue(notificationType, recipientEmail, subject, content);
        notification.setPriority(1); // Highest priority
        notification = notificationQueueRepository.save(notification);
        
        // Create legacy notification
        createLegacyNotification(notification);
        
        return notification;
    }

    /**
     * Get pending notifications ready to be processed
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getPendingNotifications() {
        return notificationQueueRepository.findPendingNotifications(NotificationStatus.PENDING, new Date());
    }

    /**
     * Get failed notifications that can be retried
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getRetryableNotifications() {
        return notificationQueueRepository.findRetryableNotifications(NotificationStatus.FAILED, new Date());
    }

    /**
     * Get notifications by status
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getNotificationsByStatus(NotificationStatus status) {
        return notificationQueueRepository.findByStatus(status);
    }

    /**
     * Get notifications by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<NotificationQueue> getNotificationsByStatus(NotificationStatus status, Pageable pageable) {
        return notificationQueueRepository.findByStatus(status, pageable);
    }

    /**
     * Get notifications by recipient email
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getNotificationsByRecipient(String recipientEmail) {
        return notificationQueueRepository.findByRecipientEmail(recipientEmail);
    }

    /**
     * Get notifications by recipient email with pagination
     */
    @Transactional(readOnly = true)
    public Page<NotificationQueue> getNotificationsByRecipient(String recipientEmail, Pageable pageable) {
        return notificationQueueRepository.findByRecipientEmail(recipientEmail, pageable);
    }

    /**
     * Get notifications by entity reference
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getNotificationsByEntity(String entityReference, String entityType) {
        return notificationQueueRepository.findByEntityReferenceAndEntityType(entityReference, entityType);
    }

    /**
     * Get notification by ID
     */
    @Transactional(readOnly = true)
    public Optional<NotificationQueue> getNotificationById(Integer id) {
        return notificationQueueRepository.findById(id);
    }

    /**
     * Update notification status
     */
    public NotificationQueue updateNotificationStatus(Integer id, NotificationStatus status) {
        NotificationQueue notification = validateAndGetNotification(id);
        validateNotificationCanBeUpdated(notification, status);
        
        notification.setStatus(status);
        
        if (status == NotificationStatus.PROCESSING) {
            notification.markAsProcessing();
        } else if (status == NotificationStatus.SENT) {
            notification.markAsSent();
        }
        
        // Update legacy notification status
        updateLegacyNotificationStatus(notification);
        
        return notificationQueueRepository.save(notification);
    }

    /**
     * Mark notification as failed
     */
    public NotificationQueue markNotificationAsFailed(Integer id, String errorMessage) {
        NotificationQueue notification = validateAndGetNotification(id);
        validateNotificationCanBeUpdated(notification, NotificationStatus.FAILED);
        
        notification.markAsFailed(errorMessage);
        
        // Update legacy notification status
        updateLegacyNotificationStatus(notification);
        
        return notificationQueueRepository.save(notification);
    }

    /**
     * Retry a failed notification
     */
    public NotificationQueue retryNotification(Integer id) {
        NotificationQueue notification = validateAndGetNotification(id);
        
        if (!notification.canRetry()) {
            throw new BusinessLogicException(
                String.format("Notification %d cannot be retried - status: %s, retry count: %d/%d", 
                            notification.getId(), notification.getStatus(), 
                            notification.getRetryCount(), notification.getMaxRetries()),
                "NOTIFICATION_CANNOT_RETRY"
            );
        }
        
        notification.incrementRetry();
        
        // Update legacy notification status
        updateLegacyNotificationStatus(notification);
        
        return notificationQueueRepository.save(notification);
    }

    /**
     * Cancel a notification
     */
    public NotificationQueue cancelNotification(Integer id) {
        NotificationQueue notification = validateAndGetNotification(id);
        
        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new BusinessLogicException(
                String.format("Cannot cancel notification %d that has already been sent", notification.getId()),
                "NOTIFICATION_ALREADY_SENT"
            );
        }
        
        if (notification.getStatus() == NotificationStatus.CANCELLED) {
            throw new BusinessLogicException(
                String.format("Notification %d is already cancelled", notification.getId()),
                "NOTIFICATION_ALREADY_CANCELLED"
            );
        }
        
        notification.cancel();
        
        // Update legacy notification status
        updateLegacyNotificationStatus(notification);
        
        return notificationQueueRepository.save(notification);
    }

    /**
     * Reset stale processing notifications
     */
    public int resetStaleProcessingNotifications(int staleMinutes) {
        Date cutoffTime = new Date(System.currentTimeMillis() - (staleMinutes * 60 * 1000));
        return notificationQueueRepository.resetStaleProcessingNotifications(
            NotificationStatus.PROCESSING, 
            NotificationStatus.PENDING, 
            cutoffTime
        );
    }

    /**
     * Clean up old notifications
     */
    public int cleanupOldNotifications(int daysToKeep) {
        Date cutoffDate = new Date(System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000));
        List<NotificationStatus> completedStatuses = List.of(
            NotificationStatus.SENT, 
            NotificationStatus.FAILED_PERMANENTLY, 
            NotificationStatus.CANCELLED
        );
        return notificationQueueRepository.deleteOldNotifications(completedStatuses, cutoffDate);
    }

    /**
     * Get notification statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getNotificationStatistics() {
        List<Object[]> results = notificationQueueRepository.getNotificationStatistics();
        return results.stream().collect(
            java.util.stream.Collectors.toMap(
                row -> ((NotificationStatus) row[0]).toString(),
                row -> (Long) row[1]
            )
        );
    }

    /**
     * Get notification statistics by type
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> getNotificationStatisticsByType() {
        List<Object[]> results = notificationQueueRepository.getNotificationStatisticsByType();
        return results.stream().collect(
            java.util.stream.Collectors.groupingBy(
                row -> ((NotificationType) row[0]).toString(),
                java.util.stream.Collectors.toMap(
                    row -> ((NotificationStatus) row[1]).toString(),
                    row -> (Long) row[2]
                )
            )
        );
    }

    /**
     * Count notifications by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(NotificationStatus status) {
        return notificationQueueRepository.countByStatus(status);
    }

    /**
     * Count notifications by type and status
     */
    @Transactional(readOnly = true)
    public long countByTypeAndStatus(NotificationType notificationType, NotificationStatus status) {
        return notificationQueueRepository.countByNotificationTypeAndStatus(notificationType, status);
    }

    /**
     * Count notifications by recipient and status
     */
    @Transactional(readOnly = true)
    public long countByRecipientAndStatus(String recipientEmail, NotificationStatus status) {
        return notificationQueueRepository.countByRecipientEmailAndStatus(recipientEmail, status);
    }

    /**
     * Get notifications created within date range
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getNotificationsByDateRange(Date startDate, Date endDate) {
        return notificationQueueRepository.findByCreatedAtBetween(startDate, endDate);
    }

    /**
     * Get notifications sent within date range
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getSentNotificationsByDateRange(Date startDate, Date endDate) {
        return notificationQueueRepository.findBySentAtBetween(startDate, endDate);
    }

    /**
     * Get notifications by template name
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getNotificationsByTemplate(String templateName) {
        return notificationQueueRepository.findByTemplateName(templateName);
    }

    /**
     * Get notifications by template name and status
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getNotificationsByTemplateAndStatus(String templateName, NotificationStatus status) {
        return notificationQueueRepository.findByTemplateNameAndStatus(templateName, status);
    }

    /**
     * Get high priority pending notifications
     */
    @Transactional(readOnly = true)
    public List<NotificationQueue> getHighPriorityPendingNotifications() {
        return notificationQueueRepository.findByPriorityAndStatus(1, NotificationStatus.PENDING);
    }

    /**
     * Update notification with processing result
     */
    public NotificationQueue updateNotificationResult(Integer id, boolean success, String errorMessage) {
        Optional<NotificationQueue> optional = notificationQueueRepository.findById(id);
        if (optional.isPresent()) {
            NotificationQueue notification = optional.get();
            
            if (success) {
                notification.markAsSent();
            } else {
                notification.markAsFailed(errorMessage);
            }
            
            // Update legacy notification status
            updateLegacyNotificationStatus(notification);
            
            return notificationQueueRepository.save(notification);
        }
        return null;
    }

    /**
     * Save notification (for updating existing notifications)
     */
    public NotificationQueue saveNotification(NotificationQueue notification) {
        return notificationQueueRepository.save(notification);
    }

    /**
     * Create legacy notification for backward compatibility
     */
    private void createLegacyNotification(NotificationQueue queueNotification) {
        try {
            Notification legacyNotification = new Notification();
            legacyNotification.setReference(UUID.randomUUID().toString());
            legacyNotification.setRecipient(queueNotification.getRecipientEmail());
            legacyNotification.setMessage(queueNotification.getContent());
            legacyNotification.setNotificationType(queueNotification.getNotificationType());
            legacyNotification.setStatus("pending");
            legacyNotification.setCreatedAt(new Date());
            notificationsRepository.save(legacyNotification);
        } catch (Exception e) {
            // Log error but don't fail the queue notification creation
            System.err.println("Error creating legacy notification: " + e.getMessage());
        }
    }

    /**
     * Update legacy notification status
     */
    private void updateLegacyNotificationStatus(NotificationQueue queueNotification) {
        try {
            // Find legacy notification by recipient and message
            List<Notification> legacyNotifications = notificationsRepository.findAll();
            for (Notification legacy : legacyNotifications) {
                if (legacy.getRecipient() != null && legacy.getRecipient().equals(queueNotification.getRecipientEmail()) &&
                    legacy.getMessage() != null && legacy.getMessage().equals(queueNotification.getContent())) {
                    
                    // Map queue status to legacy status
                    String legacyStatus = mapQueueStatusToLegacyStatus(queueNotification.getStatus());
                    legacy.setStatus(legacyStatus);
                    notificationsRepository.save(legacy);
                    break;
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the queue notification update
            System.err.println("Error updating legacy notification status: " + e.getMessage());
        }
    }

    /**
     * Map queue status to legacy status
     */
    private String mapQueueStatusToLegacyStatus(NotificationStatus queueStatus) {
        switch (queueStatus) {
            case PENDING:
                return "pending";
            case PROCESSING:
                return "processing";
            case SENT:
                return "sent";
            case FAILED:
            case FAILED_PERMANENTLY:
                return "failed";
            case CANCELLED:
                return "cancelled";
            default:
                return "pending";
        }
    }

    /**
     * Validate notification input parameters
     */
    private void validateNotificationInput(NotificationType notificationType, String recipientEmail, String subject, String content) {
        if (notificationType == null) {
            throw new ValidationException("Notification type is required", "notificationType", notificationType);
        }
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new ValidationException("Recipient email is required", "recipientEmail", recipientEmail);
        }
        
        if (!isValidEmail(recipientEmail)) {
            throw new ValidationException("Invalid email format", "recipientEmail", recipientEmail);
        }
        
        if (subject == null || subject.trim().isEmpty()) {
            throw new ValidationException("Subject is required", "subject", subject);
        }
        
        if (subject.length() > MAX_SUBJECT_LENGTH) {
            throw new ValidationException("Subject must not exceed " + MAX_SUBJECT_LENGTH + " characters", "subject", subject);
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("Content is required", "content", content);
        }
    }

    /**
     * Validate template notification input parameters
     */
    private void validateTemplateNotificationInput(NotificationType notificationType, String recipientEmail, String templateName) {
        if (notificationType == null) {
            throw new ValidationException("Notification type is required", "notificationType", notificationType);
        }
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new ValidationException("Recipient email is required", "recipientEmail", recipientEmail);
        }
        
        if (!isValidEmail(recipientEmail)) {
            throw new ValidationException("Invalid email format", "recipientEmail", recipientEmail);
        }
        
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new ValidationException("Template name is required", "templateName", templateName);
        }
        
        if (templateName.length() > MAX_TEMPLATE_NAME_LENGTH) {
            throw new ValidationException("Template name must not exceed " + MAX_TEMPLATE_NAME_LENGTH + " characters", "templateName", templateName);
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    /**
     * Validate notification ID exists
     */
    private NotificationQueue validateAndGetNotification(Integer id) {
        if (id == null) {
            throw new ValidationException("Notification ID is required", "id", id);
        }
        
        return notificationQueueRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }

    /**
     * Validate notification can be updated
     */
    private void validateNotificationCanBeUpdated(NotificationQueue notification, NotificationStatus newStatus) {
        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new BusinessLogicException(
                String.format("Cannot update notification %d that has already been sent", notification.getId()),
                "NOTIFICATION_ALREADY_SENT"
            );
        }
        
        if (notification.getStatus() == NotificationStatus.CANCELLED) {
            throw new BusinessLogicException(
                String.format("Cannot update notification %d that has been cancelled", notification.getId()),
                "NOTIFICATION_CANCELLED"
            );
        }
        
        if (notification.getStatus() == NotificationStatus.FAILED_PERMANENTLY) {
            throw new BusinessLogicException(
                String.format("Cannot update notification %d that has failed permanently", notification.getId()),
                "NOTIFICATION_FAILED_PERMANENTLY"
            );
        }
    }
}
