package org.paykesho.clientportal.notifications.dto;

public class NotificationRequest {
    private String recipient;
    private String message;
    private String notification_type;

    // Constructor
    public NotificationRequest(String recipient, String message, String notification_type) {
        this.recipient = recipient;
        this.message = message;
        this.notification_type = notification_type;
    }

    // Getters and Setters
    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNotification_type() {
        return notification_type;
    }

    public void setNotification_type(String notification_type) {
        this.notification_type = notification_type;
    }
}

