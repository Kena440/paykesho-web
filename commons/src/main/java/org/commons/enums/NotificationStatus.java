package org.commons.enums;

/**
 * Enum representing the status of notifications in the queue
 */
public enum NotificationStatus {
    /**
     * Notification has been queued but not yet processed
     */
    PENDING("Pending"),
    
    /**
     * Notification is currently being processed/sent
     */
    PROCESSING("Processing"),
    
    /**
     * Notification has been successfully sent
     */
    SENT("Sent"),
    
    /**
     * Notification failed to send but can be retried
     */
    FAILED("Failed"),
    
    /**
     * Notification failed and has exceeded retry limit
     */
    FAILED_PERMANENTLY("Failed Permanently"),
    
    /**
     * Notification was cancelled before being sent
     */
    CANCELLED("Cancelled");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
