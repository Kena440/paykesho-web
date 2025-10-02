package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity for notification requests created by any module
 * This replaces HTTP-based communication with database-driven approach
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification_requests", indexes = {
    @Index(name = "idx_notification_request_status", columnList = "status"),
    @Index(name = "idx_notification_request_type", columnList = "notification_type"),
    @Index(name = "idx_notification_request_recipient", columnList = "recipient_email"),
    @Index(name = "idx_notification_request_module", columnList = "source_module"),
    @Index(name = "idx_notification_request_priority", columnList = "priority")
})
public class NotificationRequest extends IdBasedEntity {

    /**
     * When the notification request was created
     */
    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    /**
     * When the notification should be processed (for scheduling)
     */
    @Column(name = "scheduled_for")
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledFor;

    /**
     * When the notification was last processed
     */
    @Column(name = "last_processed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastProcessedAt;

    /**
     * Type of notification (EMAIL, SMS, PUSH, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType;

    /**
     * Current status of the notification request
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * Priority level (1=highest, 5=lowest)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 3;

    /**
     * Recipient's email address
     */
    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    /**
     * Recipient's phone number (for SMS)
     */
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

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
     * Template variables as JSON string
     */
    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    /**
     * Source module that created this request
     */
    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    /**
     * Reference to the entity that triggered this notification
     */
    @Column(name = "entity_reference", length = 100)
    private String entityReference;

    /**
     * Type of entity that triggered this notification
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Additional metadata as JSON string
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Number of processing attempts
     */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /**
     * Maximum number of retry attempts
     */
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    /**
     * Last error message (if any)
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * When the notification was successfully sent
     */
    @Column(name = "sent_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    /**
     * When the notification failed permanently
     */
    @Column(name = "failed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date failedAt;

    /**
     * Constructor for creating notification requests
     */
    public NotificationRequest(NotificationType notificationType, String recipientEmail, 
                             String subject, String content, String sourceModule) {
        this.notificationType = notificationType;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.sourceModule = sourceModule;
        this.createdAt = new Date();
        this.status = NotificationStatus.PENDING;
        this.priority = 3;
        this.attemptCount = 0;
        this.maxAttempts = 3;
    }

    /**
     * Constructor for high priority notifications
     */
    public NotificationRequest(NotificationType notificationType, String recipientEmail, 
                             String subject, String content, String sourceModule, Integer priority) {
        this(notificationType, recipientEmail, subject, content, sourceModule);
        this.priority = priority;
    }

    /**
     * Constructor for scheduled notifications
     */
    public NotificationRequest(NotificationType notificationType, String recipientEmail, 
                             String subject, String content, String sourceModule, Date scheduledFor) {
        this(notificationType, recipientEmail, subject, content, sourceModule);
        this.scheduledFor = scheduledFor;
    }

    /**
     * Check if notification is ready for processing
     */
    public boolean isReadyForProcessing() {
        if (status != NotificationStatus.PENDING) {
            return false;
        }
        
        if (scheduledFor != null && scheduledFor.after(new Date())) {
            return false;
        }
        
        return attemptCount < maxAttempts;
    }

    /**
     * Check if notification has failed permanently
     */
    public boolean isFailedPermanently() {
        return status == NotificationStatus.FAILED_PERMANENTLY || 
               (status == NotificationStatus.FAILED && attemptCount >= maxAttempts);
    }

    /**
     * Mark notification as processing
     */
    public void markAsProcessing() {
        this.status = NotificationStatus.PROCESSING;
        this.lastProcessedAt = new Date();
        this.attemptCount++;
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
        this.lastError = errorMessage;
        this.lastProcessedAt = new Date();
        
        if (attemptCount >= maxAttempts) {
            this.status = NotificationStatus.FAILED_PERMANENTLY;
            this.failedAt = new Date();
        } else {
            this.status = NotificationStatus.FAILED;
        }
    }
}
