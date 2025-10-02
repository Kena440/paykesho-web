package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity representing notifications in the queue system
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification_queue", indexes = {
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_recipient", columnList = "recipient_email")
})
public class NotificationQueue extends IdBasedEntity {

    /**
     * When the notification was created
     */
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * Type of notification (EMAIL, SMS, PUSH, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType;

    /**
     * Current status of the notification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * Recipient's email address
     */
    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    /**
     * Recipient's name (optional)
     */
    @Column(name = "recipient_name", length = 128)
    private String recipientName;

    /**
     * Subject of the notification
     */
    @Column(name = "subject", length = 255)
    private String subject;

    /**
     * Content/body of the notification
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Template name used for this notification (if applicable)
     */
    @Column(name = "template_name", length = 100)
    private String templateName;

    /**
     * JSON string containing template variables
     */
    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    /**
     * Priority level (1 = highest, 5 = lowest)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 3;

    /**
     * Number of retry attempts made
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * Maximum number of retry attempts allowed
     */
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    /**
     * When the notification should be sent (for scheduling)
     */
    @Column(name = "scheduled_for")
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledFor;

    /**
     * When the notification was actually sent
     */
    @Column(name = "sent_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    /**
     * When processing of this notification started
     */
    @Column(name = "processing_started_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date processingStartedAt;

    /**
     * Error message if the notification failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Additional metadata as JSON string
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Reference to the entity that triggered this notification (e.g., event registration ID)
     */
    @Column(name = "entity_reference", length = 100)
    private String entityReference;

    /**
     * Type of entity that triggered this notification
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Constructor for creating a new notification with content
     */
    public NotificationQueue(NotificationType notificationType, String recipientEmail, 
                           String subject, String content) {
        this.notificationType = notificationType;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.status = NotificationStatus.PENDING;
        this.priority = 3;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.createdAt = new Date();
    }

    /**
     * Constructor for template-based notifications
     */
    public NotificationQueue(NotificationType notificationType, String recipientEmail, 
                           String templateName, String templateVariables, boolean isTemplate) {
        this.notificationType = notificationType;
        this.recipientEmail = recipientEmail;
        this.templateName = templateName;
        this.templateVariables = templateVariables;
        this.status = NotificationStatus.PENDING;
        this.priority = 3;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.createdAt = new Date();
    }

    /**
     * Check if the notification can be retried
     */
    public boolean canRetry() {
        return this.status == NotificationStatus.FAILED && 
               this.retryCount < this.maxRetries;
    }

    /**
     * Increment retry count and update status
     */
    public void incrementRetry() {
        this.retryCount++;
        if (this.retryCount >= this.maxRetries) {
            this.status = NotificationStatus.FAILED_PERMANENTLY;
        } else {
            this.status = NotificationStatus.PENDING;
        }
    }

    /**
     * Mark notification as processing
     */
    public void markAsProcessing() {
        this.status = NotificationStatus.PROCESSING;
        this.processingStartedAt = new Date();
    }

    /**
     * Mark notification as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = new Date();
    }

    /**
     * Mark notification as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
        
        if (this.retryCount >= this.maxRetries) {
            this.status = NotificationStatus.FAILED_PERMANENTLY;
        }
    }

    /**
     * Cancel the notification
     */
    public void cancel() {
        this.status = NotificationStatus.CANCELLED;
    }

    /**
     * Check if notification is scheduled for future processing
     */
    public boolean isScheduled() {
        return scheduledFor != null;
    }

    /**
     * Set createdAt timestamp before persisting new entities
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
