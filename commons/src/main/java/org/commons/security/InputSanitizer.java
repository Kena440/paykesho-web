package org.commons.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent XSS, SQL Injection, and other attacks
 */
public class InputSanitizer {
    
    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);
    
    // Patterns for detecting malicious input
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONERROR_PATTERN = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    
    // Whitelisted characters for different types of input
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s'-]+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s,.-]+$");
    
    /**
     * Sanitizes general text input by removing HTML tags and dangerous characters
     */
    public static String sanitizeText(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String sanitized = input;
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove event handlers (onclick, onerror, etc.)
        sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");
        
        // HTML encode special characters
        sanitized = encodeHtmlEntities(sanitized);
        
        if (!sanitized.equals(input)) {
            log.warn("Potentially malicious input detected and sanitized: {}", input.substring(0, Math.min(50, input.length())));
        }
        
        return sanitized.trim();
    }
    
    /**
     * Sanitizes name input (letters, spaces, hyphens, apostrophes only)
     */
    public static String sanitizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        
        String sanitized = sanitizeText(name);
        
        // Additional validation for names
        if (!NAME_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid characters in name field: {}", name);
            // Remove invalid characters
            sanitized = sanitized.replaceAll("[^a-zA-Z\\s'-]", "");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Sanitizes address input
     */
    public static String sanitizeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return address;
        }
        
        String sanitized = sanitizeText(address);
        
        // Validate address pattern
        if (!ADDRESS_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid characters in address field: {}", address);
            // Remove invalid characters
            sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s,.-]", "");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Sanitizes phone number input
     */
    public static String sanitizePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        
        String sanitized = phone.replaceAll("[^0-9+\\-\\s()]", "");
        
        if (!sanitized.equals(phone)) {
            log.warn("Invalid characters removed from phone number");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Sanitizes NRC input
     */
    public static String sanitizeNRC(String nrc) {
        if (nrc == null || nrc.trim().isEmpty()) {
            return nrc;
        }
        
        String sanitized = nrc.replaceAll("[^0-9/]", "");
        
        if (!sanitized.equals(nrc)) {
            log.warn("Invalid characters removed from NRC");
        }
        
        return sanitized.trim();
    }
    
    /**
     * Sanitizes numeric input (TPIN, MAN number, etc.)
     */
    public static String sanitizeNumeric(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String sanitized = input.replaceAll("[^0-9]", "");
        
        if (!sanitized.equals(input)) {
            log.warn("Non-numeric characters removed from input");
        }
        
        return sanitized;
    }
    
    /**
     * Checks if input contains potential XSS attack patterns
     */
    public static boolean containsXSSPatterns(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Check for script tags
        if (lowerInput.contains("<script") || lowerInput.contains("</script>")) {
            return true;
        }
        
        // Check for javascript: protocol
        if (lowerInput.contains("javascript:")) {
            return true;
        }
        
        // Check for event handlers
        if (lowerInput.matches(".*on\\w+\\s*=.*")) {
            return true;
        }
        
        // Check for iframe, embed, object tags
        if (lowerInput.contains("<iframe") || lowerInput.contains("<embed") || 
            lowerInput.contains("<object") || lowerInput.contains("<applet")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if input contains potential SQL injection patterns
     */
    public static boolean containsSQLInjectionPatterns(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Check for common SQL injection keywords
        String[] sqlKeywords = {
            "union", "select", "insert", "update", "delete", "drop", 
            "create", "alter", "exec", "execute", "script", "--", "/*", "*/"
        };
        
        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Encodes HTML special characters to prevent XSS
     */
    private static String encodeHtmlEntities(String input) {
        if (input == null) {
            return null;
        }
        
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }
    
    /**
     * Validates and sanitizes email input
     */
    public static String sanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }
        
        String sanitized = email.toLowerCase().trim();
        
        // Remove any HTML tags or scripts
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Basic email validation
        if (!sanitized.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            log.warn("Invalid email format detected: {}", email);
        }
        
        return sanitized;
    }
    
    /**
     * Detects and logs suspicious activity
     */
    public static void detectSuspiciousActivity(String fieldName, String input) {
        if (input == null) {
            return;
        }
        
        if (containsXSSPatterns(input)) {
            log.error("XSS ATTACK DETECTED in field '{}': {}", fieldName, input.substring(0, Math.min(100, input.length())));
        }
        
        if (containsSQLInjectionPatterns(input)) {
            log.error("SQL INJECTION ATTEMPT DETECTED in field '{}': {}", fieldName, input.substring(0, Math.min(100, input.length())));
        }
    }
}

