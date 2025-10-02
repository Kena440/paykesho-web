package org.commons.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final String resourceType;
    private final Object resourceId;
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s with id %s not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String resourceType, Object resourceId, Throwable cause) {
        super(String.format("%s with id %s not found", resourceType, resourceId), cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public Object getResourceId() {
        return resourceId;
    }
}
