package org.paykesho.backoffice.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.SecurityAudit;
import org.commons.enums.SecurityAuditStatus;
import org.paykesho.backoffice.security.repository.SecurityAuditRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Service to handle actions on security audit findings
 * Allows admins to delete malicious records or mark as false positives
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityActionService {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityAuditRepository securityAuditRepository;

    /**
     * Delete malicious record from its table
     */
    @Transactional
    public boolean deleteMaliciousRecord(String tableName, Integer recordId) {
        try {
            // Security check - only allow deletion from specific tables
            if (!isValidTableName(tableName)) {
                log.error("Attempted to delete from invalid table: {}", tableName);
                return false;
            }
            
            String sql = "DELETE FROM " + tableName + " WHERE id = ?";
            int deleted = jdbcTemplate.update(sql, recordId);
            
            if (deleted > 0) {
                log.info("Deleted malicious record: table={}, id={}", tableName, recordId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error deleting malicious record from {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Mark audit as resolved (record deleted)
     */
    @Transactional
    public void markAsResolved(Integer auditId, String actionTaken) {
        SecurityAudit audit = securityAuditRepository.findById(auditId).orElse(null);
        
        if (audit != null) {
            audit.setStatus(SecurityAuditStatus.RESOLVED);
            audit.setActionTaken(actionTaken);
            audit.setReviewedDate(new Date());
            // TODO: Set reviewedBy to current user ID
            securityAuditRepository.save(audit);
            
            log.info("Marked audit {} as resolved", auditId);
        }
    }

    /**
     * Mark audit as false positive
     */
    @Transactional
    public void markAsFalsePositive(Integer auditId, String reason) {
        SecurityAudit audit = securityAuditRepository.findById(auditId).orElse(null);
        
        if (audit != null) {
            audit.setStatus(SecurityAuditStatus.FALSE_POSITIVE);
            audit.setActionTaken(reason);
            audit.setReviewedDate(new Date());
            securityAuditRepository.save(audit);
            
            log.info("Marked audit {} as false positive", auditId);
        }
    }

    /**
     * Mark audit as in progress
     */
    @Transactional
    public void markAsInProgress(Integer auditId) {
        SecurityAudit audit = securityAuditRepository.findById(auditId).orElse(null);
        
        if (audit != null) {
            audit.setStatus(SecurityAuditStatus.IN_PROGRESS);
            audit.setReviewedDate(new Date());
            securityAuditRepository.save(audit);
            
            log.info("Marked audit {} as in progress", auditId);
        }
    }

    /**
     * Validate table name to prevent SQL injection
     */
    private boolean isValidTableName(String tableName) {
        String[] allowedTables = {
            "clients", "notifications", "notification_queue", "file_uploads",
            "users", "organization_admins", "merchant_admins",
            "organizations", "merchants", "employee_records"
        };
        
        for (String allowed : allowedTables) {
            if (allowed.equals(tableName)) {
                return true;
            }
        }
        
        return false;
    }
}

