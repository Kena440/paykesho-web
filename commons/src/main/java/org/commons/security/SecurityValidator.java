package org.commons.security;

import org.commons.entity.client.Client;
import org.commons.entity.NotificationQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security validation service for detecting and preventing malicious input
 */
public class SecurityValidator {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityValidator.class);
    
    /**
     * Validates and sanitizes all client input fields
     * @param client The client object to validate
     * @throws SecurityException if malicious content is detected
     */
    public static void validateAndSanitizeClient(Client client) throws SecurityException {
        if (client == null) {
            return;
        }
        
        // Detect suspicious activity first
        detectMaliciousInput(client);
        
        // Sanitize all text fields
        if (client.getName() != null) {
            client.setName(InputSanitizer.sanitizeName(client.getName()));
        }
        
        if (client.getEmail() != null) {
            client.setEmail(InputSanitizer.sanitizeEmail(client.getEmail()));
        }
        
        if (client.getAddress() != null) {
            client.setAddress(InputSanitizer.sanitizeAddress(client.getAddress()));
        }
        
        if (client.getPhoneNumber() != null) {
            client.setPhoneNumber(InputSanitizer.sanitizePhoneNumber(client.getPhoneNumber()));
        }
        
        if (client.getNrc() != null) {
            client.setNrc(InputSanitizer.sanitizeNRC(client.getNrc()));
        }
        
        if (client.getTpin() != null) {
            client.setTpin(InputSanitizer.sanitizeNumeric(client.getTpin()));
        }
        
        if (client.getManNumber() != null) {
            client.setManNumber(InputSanitizer.sanitizeNumeric(client.getManNumber()));
        }
    }
    
    /**
     * Detects malicious input patterns and throws exception if found
     */
    private static void detectMaliciousInput(Client client) throws SecurityException {
        StringBuilder violations = new StringBuilder();
        boolean isMalicious = false;
        
        // Check name field
        if (client.getName() != null) {
            if (InputSanitizer.containsXSSPatterns(client.getName())) {
                violations.append("XSS pattern detected in name field. ");
                isMalicious = true;
                log.error("SECURITY ALERT: XSS attempt in name field: {}", client.getName());
            }
            if (InputSanitizer.containsSQLInjectionPatterns(client.getName())) {
                violations.append("SQL injection pattern detected in name field. ");
                isMalicious = true;
                log.error("SECURITY ALERT: SQL injection attempt in name field: {}", client.getName());
            }
        }
        
        // Check email field
        if (client.getEmail() != null) {
            if (InputSanitizer.containsXSSPatterns(client.getEmail())) {
                violations.append("XSS pattern detected in email field. ");
                isMalicious = true;
                log.error("SECURITY ALERT: XSS attempt in email field: {}", client.getEmail());
            }
        }
        
        // Check address field
        if (client.getAddress() != null) {
            if (InputSanitizer.containsXSSPatterns(client.getAddress())) {
                violations.append("XSS pattern detected in address field. ");
                isMalicious = true;
                log.error("SECURITY ALERT: XSS attempt in address field: {}", client.getAddress());
            }
            // Check if address contains URL (possible spam)
            if (client.getAddress().toLowerCase().matches(".*https?://.*")) {
                violations.append("URL detected in address field. ");
                isMalicious = true;
                log.error("SECURITY ALERT: URL in address field: {}", client.getAddress());
            }
        }
        
        // Check NRC field
        if (client.getNrc() != null) {
            if (InputSanitizer.containsXSSPatterns(client.getNrc())) {
                violations.append("XSS pattern detected in NRC field. ");
                isMalicious = true;
                log.error("SECURITY ALERT: XSS attempt in NRC field: {}", client.getNrc());
            }
            // NRC should only contain numbers and slashes
            if (!client.getNrc().matches("^[0-9/]+$")) {
                violations.append("Invalid characters in NRC field. ");
                log.warn("Invalid NRC format: {}", client.getNrc());
            }
        }
        
        if (isMalicious) {
            throw new SecurityException("Malicious input detected: " + violations.toString());
        }
    }
    
    /**
     * Validates individual field for malicious content
     */
    public static void validateField(String fieldName, String value) throws SecurityException {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        
        if (InputSanitizer.containsXSSPatterns(value)) {
            log.error("SECURITY ALERT: XSS attempt in field '{}': {}", fieldName, value);
            throw new SecurityException("XSS pattern detected in " + fieldName);
        }
        
        if (InputSanitizer.containsSQLInjectionPatterns(value)) {
            log.error("SECURITY ALERT: SQL injection attempt in field '{}': {}", fieldName, value);
            throw new SecurityException("SQL injection pattern detected in " + fieldName);
        }
    }
    
    /**
     * Checks if a string is safe (no malicious patterns)
     */
    public static boolean isSafeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        
        return !InputSanitizer.containsXSSPatterns(input) && 
               !InputSanitizer.containsSQLInjectionPatterns(input);
    }
    
    // =====================================================
    // NOTIFICATION QUEUE VALIDATION (CRITICAL - QUEUED MESSAGES)
    // =====================================================
    
    public static void validateAndSanitizeNotificationQueue(NotificationQueue notification) throws SecurityException {
        if (notification == null) {
            return;
        }
        
        if (notification.getRecipientEmail() != null) {
            validateField("notification queue recipient email", notification.getRecipientEmail());
            notification.setRecipientEmail(InputSanitizer.sanitizeEmail(notification.getRecipientEmail()));
        }
        
        if (notification.getRecipientName() != null) {
            validateField("notification queue recipient name", notification.getRecipientName());
            notification.setRecipientName(InputSanitizer.sanitizeName(notification.getRecipientName()));
        }
        
        if (notification.getSubject() != null) {
            validateField("notification queue subject", notification.getSubject());
            notification.setSubject(InputSanitizer.sanitizeText(notification.getSubject()));
        }
        
        if (notification.getContent() != null) {
            validateField("notification queue content", notification.getContent());
            notification.setContent(InputSanitizer.sanitizeText(notification.getContent()));
        }
    }
}

