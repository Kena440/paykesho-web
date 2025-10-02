package org.commons.exception;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RuntimeException {
    
    private final String field;
    private final Object value;
    
    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }
    
    public ValidationException(String message, String field, Object value) {
        super(message);
        this.field = field;
        this.value = value;
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.value = null;
    }
    
    public ValidationException(String message, String field, Object value, Throwable cause) {
        super(message, cause);
        this.field = field;
        this.value = value;
    }
    
    public String getField() {
        return field;
    }
    
    public Object getValue() {
        return value;
    }
}
