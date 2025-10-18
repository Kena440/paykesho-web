package org.paykesho.backoffice.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.SecurityAudit;
import org.commons.enums.SecurityAuditStatus;
import org.commons.enums.SecurityThreatType;
import org.paykesho.backoffice.security.repository.SecurityAuditRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Security Audit Service - Scans database for malicious data
 * and saves findings to security_audits table for admin review
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityAuditRepository securityAuditRepository;

    /**
     * Runs daily security audit at 2 AM
     * Scans all entities and saves findings to database
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @Transactional
    public void runDailySecurityAudit() {
        log.info("Starting daily security audit at {}", LocalDateTime.now());
        
        int totalFindings = 0;
        
        // Scan each entity
        totalFindings += scanClients();
        totalFindings += scanNotifications();
        totalFindings += scanNotificationQueue();
        totalFindings += scanFileUploads();
        totalFindings += scanUsers();
        totalFindings += scanOrganizationAdmins();
        totalFindings += scanMerchantAdmins();
        totalFindings += scanOrganizations();
        totalFindings += scanMerchants();
        totalFindings += scanEmployeeRecords();
        
        if (totalFindings > 0) {
            log.error("SECURITY AUDIT ALERT: Found {} suspicious records. Check backoffice security dashboard.", totalFindings);
        } else {
            log.info("Security audit completed: No suspicious data found");
        }
        
        log.info("Daily security audit completed at {}", LocalDateTime.now());
    }

    /**
     * Scan clients table for malicious data
     */
    private int scanClients() {
        int findings = 0;
        
        // XSS in name field
        String sql = "SELECT id, name FROM clients WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+='";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String name = (String) row.get("name");
            
            // Check if already reported
            if (isAlreadyReported("clients", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "clients", id, SecurityThreatType.XSS_ATTACK,
                "name", truncate(name, 1000),
                "XSS pattern detected in client name field",
                "CRITICAL"
            );
            securityAuditRepository.save(audit);
            findings++;
            log.warn("SECURITY: XSS detected in client ID {} name: {}", id, truncate(name, 50));
        }
        
        // URLs in address field
        sql = "SELECT id, address FROM clients WHERE address REGEXP 'https?://'";
        results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String address = (String) row.get("address");
            
            if (isAlreadyReported("clients", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "clients", id, SecurityThreatType.URL_IN_ADDRESS,
                "address", truncate(address, 1000),
                "URL detected in address field - possible spam",
                "HIGH"
            );
            securityAuditRepository.save(audit);
            findings++;
            log.warn("SECURITY: URL in client ID {} address: {}", id, truncate(address, 50));
        }
        
        // Invalid NRC format
        sql = "SELECT id, nrc FROM clients WHERE nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$' AND nrc IS NOT NULL AND nrc != ''";
        results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String nrc = (String) row.get("nrc");
            
            if (isAlreadyReported("clients", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "clients", id, SecurityThreatType.INVALID_FORMAT,
                "nrc", truncate(nrc, 1000),
                "NRC format should be XXXXXX/XX/X",
                "MEDIUM"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }


    /**
     * Scan notifications table
     */
    private int scanNotifications() {
        int findings = 0;
        
        String sql = "SELECT id, message FROM notifications WHERE message REGEXP '<[^>]*>|javascript:|<script'";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String message = (String) row.get("message");
            
            if (isAlreadyReported("notifications", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "notifications", id, SecurityThreatType.XSS_ATTACK,
                "message", truncate(message, 1000),
                "XSS pattern in notification message sent to users",
                "CRITICAL"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }

    /**
     * Scan file_uploads table
     */
    private int scanFileUploads() {
        int findings = 0;
        
        // Path traversal
        String sql = "SELECT id, file_name, file_path FROM file_uploads " +
                    "WHERE file_name LIKE '%..%' OR file_path LIKE '%..%'";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Long id = ((Number) row.get("id")).longValue();
            String fileName = (String) row.get("file_name");
            String filePath = (String) row.get("file_path");
            
            if (isAlreadyReportedLong("file_uploads", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "file_uploads", id.intValue(), SecurityThreatType.PATH_TRAVERSAL,
                "file_name/file_path", truncate(fileName + " | " + filePath, 1000),
                "Path traversal attempt detected",
                "CRITICAL"
            );
            securityAuditRepository.save(audit);
            findings++;
            log.error("SECURITY: Path traversal in file upload ID {}", id);
        }
        
        return findings;
    }

    /**
     * Scan users table (admin accounts)
     */
    private int scanUsers() {
        int findings = 0;
        
        String sql = "SELECT id, first_name, last_name FROM users " +
                    "WHERE first_name REGEXP '<[^>]*>|<script' OR last_name REGEXP '<[^>]*>|<script'";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String firstName = (String) row.get("first_name");
            String lastName = (String) row.get("last_name");
            
            if (isAlreadyReported("users", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "users", id, SecurityThreatType.XSS_ATTACK,
                "first_name/last_name", truncate(firstName + " " + lastName, 1000),
                "XSS in admin user account - HIGH RISK",
                "CRITICAL"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }

    /**
     * Scan organization_admins table
     */
    private int scanOrganizationAdmins() {
        return scanAdminTable("organization_admins", "Organization Admin");
    }

    /**
     * Scan merchant_admins table
     */
    private int scanMerchantAdmins() {
        return scanAdminTable("merchant_admins", "Merchant Admin");
    }

    /**
     * Generic method to scan admin tables
     */
    private int scanAdminTable(String tableName, String description) {
        int findings = 0;
        
        String sql = "SELECT id, name FROM " + tableName + " WHERE name REGEXP '<[^>]*>|<script'";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String name = (String) row.get("name");
            
            if (isAlreadyReported(tableName, id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                tableName, id, SecurityThreatType.XSS_ATTACK,
                "name", truncate(name, 1000),
                "XSS in " + description + " account",
                "HIGH"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }

    /**
     * Scan organizations table
     */
    private int scanOrganizations() {
        int findings = 0;
        
        String sql = "SELECT id, name FROM organizations WHERE name REGEXP '<[^>]*>|<script'";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String name = (String) row.get("name");
            
            if (isAlreadyReported("organizations", id)) {
                continue;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "organizations", id, SecurityThreatType.XSS_ATTACK,
                "name", truncate(name, 1000),
                "XSS in organization name",
                "MEDIUM"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }

    /**
     * Scan merchants table
     */
    private int scanMerchants() {
        int findings = 0;
        
        String sql = "SELECT id, name, physical_address FROM merchants " +
                    "WHERE name REGEXP '<[^>]*>|<script' OR physical_address REGEXP 'https?://'";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String name = (String) row.get("name");
            String address = (String) row.get("physical_address");
            
            if (isAlreadyReported("merchants", id)) {
                continue;
            }
            
            SecurityThreatType threatType = name != null && name.matches(".*<[^>]*>.*") 
                ? SecurityThreatType.XSS_ATTACK 
                : SecurityThreatType.URL_IN_ADDRESS;
            
            SecurityAudit audit = new SecurityAudit(
                "merchants", id, threatType,
                "name/physical_address", truncate(name + " | " + address, 1000),
                threatType == SecurityThreatType.XSS_ATTACK ? "XSS in merchant data" : "URL in merchant address",
                "MEDIUM"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }

    /**
     * Scan employee_records table
     */
    private int scanEmployeeRecords() {
        int findings = 0;
        
        String sql = "SELECT id, name, nrc FROM employee_records " +
                    "WHERE name REGEXP '<[^>]*>|<script' " +
                    "   OR (nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$' AND nrc IS NOT NULL)";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String name = (String) row.get("name");
            String nrc = (String) row.get("nrc");
            
            if (isAlreadyReported("employee_records", id)) {
                continue;
            }
            
            SecurityThreatType threatType = name != null && name.matches(".*<[^>]*>.*")
                ? SecurityThreatType.XSS_ATTACK
                : SecurityThreatType.INVALID_FORMAT;
            
            SecurityAudit audit = new SecurityAudit(
                "employee_records", id, threatType,
                threatType == SecurityThreatType.XSS_ATTACK ? "name" : "nrc",
                truncate(threatType == SecurityThreatType.XSS_ATTACK ? name : nrc, 1000),
                threatType == SecurityThreatType.XSS_ATTACK ? "XSS in employee name" : "Invalid NRC format",
                "MEDIUM"
            );
            securityAuditRepository.save(audit);
            findings++;
        }
        
        return findings;
    }

    /**
     * Scan notification_queue table
     */
    private int scanNotificationQueue() {
        int findings = 0;
        
        String sql = "SELECT id, recipient_name, subject, content FROM notification_queue " +
                    "WHERE (recipient_name REGEXP '<[^>]*>|<script' AND recipient_name IS NOT NULL) " +
                    "   OR (subject REGEXP '<[^>]*>|<script' AND subject IS NOT NULL) " +
                    "   OR (content REGEXP '<[^>]*>|<script' AND content IS NOT NULL)";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            Integer id = (Integer) row.get("id");
            String recipientName = (String) row.get("recipient_name");
            String subject = (String) row.get("subject");
            String content = (String) row.get("content");
            
            if (isAlreadyReported("notification_queue", id)) {
                continue;
            }
            
            // Determine which field has the issue
            String suspiciousField = "content";
            String suspiciousValue = content;
            
            if (recipientName != null && recipientName.matches(".*<[^>]*>.*")) {
                suspiciousField = "recipient_name";
                suspiciousValue = recipientName;
            } else if (subject != null && subject.matches(".*<[^>]*>.*")) {
                suspiciousField = "subject";
                suspiciousValue = subject;
            }
            
            SecurityAudit audit = new SecurityAudit(
                "notification_queue", id, SecurityThreatType.XSS_ATTACK,
                suspiciousField, truncate(suspiciousValue, 1000),
                "XSS pattern in queued notification - will be sent to users!",
                "CRITICAL"
            );
            securityAuditRepository.save(audit);
            findings++;
            log.error("SECURITY: XSS in notification queue ID {} - CRITICAL: Will be sent to users!", id);
        }
        
        return findings;
    }

    /**
     * Check if record is already reported (avoid duplicates)
     */
    private boolean isAlreadyReported(String tableName, Integer recordId) {
        List<SecurityAudit> existing = securityAuditRepository
            .findByTableNameAndRecordId(tableName, recordId);
        return !existing.isEmpty() && existing.stream()
            .anyMatch(a -> a.getStatus() == SecurityAuditStatus.PENDING);
    }

    private boolean isAlreadyReportedLong(String tableName, Long recordId) {
        return isAlreadyReported(tableName, recordId.intValue());
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * Get all pending audits
     */
    public List<SecurityAudit> getPendingAudits() {
        return securityAuditRepository.findPendingOrderBySeverity();
    }

    /**
     * Get audit by ID
     */
    public SecurityAudit getAuditById(Integer id) {
        return securityAuditRepository.findById(id).orElse(null);
    }

    /**
     * Get count of pending audits
     */
    public Long countPending() {
        return securityAuditRepository.countPendingAudits();
    }
}

