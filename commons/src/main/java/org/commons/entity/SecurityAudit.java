package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.commons.enums.SecurityThreatType;
import org.commons.enums.SecurityAuditStatus;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity to store security audit findings
 * Allows admins to review and take action on suspicious data
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "security_audits")
public class SecurityAudit extends IdBasedEntity {
    
    @Column(name = "scan_date", nullable = false)
    private Date scanDate;
    
    @Column(name = "table_name", length = 50, nullable = false)
    private String tableName;
    
    @Column(name = "record_id", nullable = false)
    private Integer recordId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", length = 50, nullable = false)
    private SecurityThreatType threatType;
    
    @Column(name = "field_name", length = 50)
    private String fieldName;
    
    @Column(name = "suspicious_value", length = 1000)
    private String suspiciousValue;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SecurityAuditStatus status;
    
    @Column(name = "reviewed_by")
    private Integer reviewedBy;
    
    @Column(name = "reviewed_date")
    private Date reviewedDate;
    
    @Column(name = "action_taken", length = 500)
    private String actionTaken;
    
    @Column(name = "severity", length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    public SecurityAudit(String tableName, Integer recordId, SecurityThreatType threatType, 
                        String fieldName, String suspiciousValue, String description, String severity) {
        this.scanDate = new Date();
        this.tableName = tableName;
        this.recordId = recordId;
        this.threatType = threatType;
        this.fieldName = fieldName;
        this.suspiciousValue = suspiciousValue;
        this.description = description;
        this.status = SecurityAuditStatus.PENDING;
        this.severity = severity;
    }
}

