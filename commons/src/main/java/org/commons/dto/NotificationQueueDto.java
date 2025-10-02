package org.commons.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;

import java.util.Date;

/**
 * DTO for NotificationQueue entity
 */
@Data
@NoArgsConstructor
public class NotificationQueueDto {

    private Integer id;
    private NotificationType notificationType;
    private NotificationStatus status;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String content;
    private String templateName;
    private String templateVariables;
    private Integer priority;
    private Integer retryCount;
    private Integer maxRetries;
    private Date scheduledFor;
    private Date sentAt;
    private Date processingStartedAt;
    private String errorMessage;
    private String metadata;
    private String entityReference;
    private String entityType;
    private Date createdAt;
    private Date updatedAt;

    /**
     * Constructor from NotificationQueue entity
     */
    public NotificationQueueDto(NotificationQueue notification) {
        this.id = notification.getId();
        this.notificationType = notification.getNotificationType();
        this.status = notification.getStatus();
        this.recipientEmail = notification.getRecipientEmail();
        this.recipientName = notification.getRecipientName();
        this.subject = notification.getSubject();
        this.content = notification.getContent();
        this.templateName = notification.getTemplateName();
        this.templateVariables = notification.getTemplateVariables();
        this.priority = notification.getPriority();
        this.retryCount = notification.getRetryCount();
        this.maxRetries = notification.getMaxRetries();
        this.scheduledFor = notification.getScheduledFor();
        this.sentAt = notification.getSentAt();
        this.processingStartedAt = notification.getProcessingStartedAt();
        this.errorMessage = notification.getErrorMessage();
        this.metadata = notification.getMetadata();
        this.entityReference = notification.getEntityReference();
        this.entityType = notification.getEntityType();
        // Note: createdAt and updatedAt are not available in IdBasedEntity
        // These fields will be null unless explicitly set
    }

    /**
     * Check if notification can be retried
     */
    public boolean canRetry() {
        return this.status == NotificationStatus.FAILED && 
               this.retryCount < this.maxRetries;
    }

    /**
     * Check if notification is scheduled for future
     */
    public boolean isScheduled() {
        return this.scheduledFor != null && this.scheduledFor.after(new Date());
    }

    /**
     * Check if notification is ready to be processed
     */
    public boolean isReadyToProcess() {
        return this.status == NotificationStatus.PENDING && 
               (this.scheduledFor == null || this.scheduledFor.before(new Date()));
    }

    /**
     * Get status display name
     */
    public String getStatusDisplayName() {
        return this.status != null ? this.status.getDisplayName() : "Unknown";
    }

    /**
     * Get type display name
     */
    public String getTypeDisplayName() {
        return this.notificationType != null ? this.notificationType.toString() : "Unknown";
    }

    /**
     * Get priority description
     */
    public String getPriorityDescription() {
        if (this.priority == null) return "Normal";
        
        switch (this.priority) {
            case 1: return "Critical";
            case 2: return "High";
            case 3: return "Normal";
            case 4: return "Low";
            case 5: return "Very Low";
            default: return "Normal";
        }
    }

    /**
     * Get retry status description
     */
    public String getRetryStatusDescription() {
        if (this.retryCount == null || this.maxRetries == null) return "No retries";
        
        if (this.retryCount == 0) return "No retries yet";
        if (this.retryCount >= this.maxRetries) return "Max retries reached";
        
        return String.format("%d of %d retries used", this.retryCount, this.maxRetries);
    }
}
