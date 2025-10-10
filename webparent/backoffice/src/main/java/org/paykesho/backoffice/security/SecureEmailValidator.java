package org.paykesho.backoffice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Secure email validation that doesn't reveal if email exists in the system
 * 
 * Security Features:
 * - Validates email format without database lookup
 * - Prevents timing attacks through consistent processing
 * - Sanitizes input to prevent injection attacks
 * - Uses secure regex patterns
 */
@Slf4j
@Component
public class SecureEmailValidator {

    // RFC 5322 compliant email regex (simplified but secure)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    // Maximum email length to prevent DoS attacks
    private static final int MAX_EMAIL_LENGTH = 254;
    
    // Minimum email length
    private static final int MIN_EMAIL_LENGTH = 5;
    
    // Common disposable email domains to block (partial list)
    private static final String[] DISPOSABLE_EMAIL_DOMAINS = {
        "10minutemail.com", "tempmail.org", "guerrillamail.com", 
        "mailinator.com", "throwaway.email", "temp-mail.org"
    };
    
    /**
     * Validate email format and security without revealing if it exists in system
     * @param email The email address to validate
     * @return EmailValidationResult with validation status and sanitized email
     */
    public EmailValidationResult validateEmail(String email) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Always process the email to maintain consistent timing
            EmailValidationResult result = performValidation(email);
            
            // Ensure minimum processing time to prevent timing attacks
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime < 50) { // Minimum 50ms processing time
                try {
                    Thread.sleep(50 - elapsedTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            log.info("Email validation completed for {} in {}ms", 
                    sanitizeEmailForLogging(email), System.currentTimeMillis() - startTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during email validation: {}", e.getMessage());
            // Always return invalid result on error to prevent information leakage
            return new EmailValidationResult(false, "Invalid email format", null);
        }
    }
    
    /**
     * Perform the actual email validation
     */
    private EmailValidationResult performValidation(String email) {
        // Check for null or empty
        if (email == null || email.trim().isEmpty()) {
            return new EmailValidationResult(false, "Email is required", null);
        }
        
        // Trim whitespace
        String trimmedEmail = email.trim();
        
        // Check length constraints
        if (trimmedEmail.length() < MIN_EMAIL_LENGTH) {
            return new EmailValidationResult(false, "Email is too short", null);
        }
        
        if (trimmedEmail.length() > MAX_EMAIL_LENGTH) {
            return new EmailValidationResult(false, "Email is too long", null);
        }
        
        // Check for basic security issues
        if (containsSuspiciousCharacters(trimmedEmail)) {
            return new EmailValidationResult(false, "Invalid email format", null);
        }
        
        // Validate email format using regex
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return new EmailValidationResult(false, "Invalid email format", null);
        }
        
        // Check for disposable email domains
        if (isDisposableEmail(trimmedEmail)) {
            return new EmailValidationResult(false, "Disposable email addresses are not allowed", null);
        }
        
        // Email passed all validation checks
        String sanitizedEmail = sanitizeEmail(trimmedEmail);
        return new EmailValidationResult(true, "Valid email format", sanitizedEmail);
    }
    
    /**
     * Check for suspicious characters that might indicate injection attempts
     */
    private boolean containsSuspiciousCharacters(String email) {
        // Check for SQL injection patterns
        String[] suspiciousPatterns = {
            "'", "\"", ";", "--", "/*", "*/", "xp_", "sp_", 
            "<script", "javascript:", "vbscript:", "onload=", "onerror="
        };
        
        String lowerEmail = email.toLowerCase();
        for (String pattern : suspiciousPatterns) {
            if (lowerEmail.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if email is from a disposable email service
     */
    private boolean isDisposableEmail(String email) {
        String domain = extractDomain(email);
        if (domain == null) return false;
        
        String lowerDomain = domain.toLowerCase();
        for (String disposableDomain : DISPOSABLE_EMAIL_DOMAINS) {
            if (lowerDomain.equals(disposableDomain)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extract domain from email address
     */
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }
    
    /**
     * Sanitize email for safe storage and processing
     */
    private String sanitizeEmail(String email) {
        if (email == null) return null;
        
        // Convert to lowercase for consistency
        String sanitized = email.trim().toLowerCase();
        
        // Remove any potential null bytes or control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        return sanitized;
    }
    
    /**
     * Sanitize email for logging purposes (hide sensitive parts)
     */
    private String sanitizeEmailForLogging(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        
        // Show first 2 characters and last 2 characters, hide the rest
        String domain = extractDomain(email);
        if (domain != null) {
            String localPart = email.substring(0, email.indexOf('@'));
            if (localPart.length() > 4) {
                return localPart.substring(0, 2) + "***@" + domain;
            } else {
                return "***@" + domain;
            }
        }
        
        return "***";
    }
    
    /**
     * Result of email validation
     */
    public static class EmailValidationResult {
        private final boolean valid;
        private final String message;
        private final String sanitizedEmail;
        
        public EmailValidationResult(boolean valid, String message, String sanitizedEmail) {
            this.valid = valid;
            this.message = message;
            this.sanitizedEmail = sanitizedEmail;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getSanitizedEmail() { return sanitizedEmail; }
    }
}
