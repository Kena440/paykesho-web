package org.commons.enums;

public enum SecurityAuditStatus {
    PENDING("Pending Review"),
    REVIEWED("Reviewed"),
    RESOLVED("Resolved - Deleted"),
    FALSE_POSITIVE("False Positive - Ignored"),
    IN_PROGRESS("In Progress");
    
    private final String description;
    
    SecurityAuditStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

