package org.commons.exception;

/**
 * Exception thrown when notification processing fails
 */
public class NotificationProcessingException extends RuntimeException {
    
    private final String errorCode;
    
    public NotificationProcessingException(String message) {
        super(message);
        this.errorCode = "NOTIFICATION_PROCESSING_ERROR";
    }
    
    public NotificationProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public NotificationProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NOTIFICATION_PROCESSING_ERROR";
    }
    
    public NotificationProcessingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
