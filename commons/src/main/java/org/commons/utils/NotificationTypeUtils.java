package org.commons.utils;

import org.commons.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for resilient NotificationType parsing
 * Handles various input formats and provides fallbacks
 */
public class NotificationTypeUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationTypeUtils.class);
    
    /**
     * Parse notification type with multiple fallback strategies
     * @param input The input string to parse
     * @return Parsed NotificationType or email as final fallback
     */
    public static NotificationType parseNotificationType(String input) {
        if (input == null || input.trim().isEmpty()) {
            logger.warn("Null or empty notification type provided, defaulting to email");
            return NotificationType.email;
        }
        
        String normalized = input.trim();
        
        // Strategy 1: Direct match (case-insensitive)
        try {
            return NotificationType.valueOf(normalized.toLowerCase());
        } catch (IllegalArgumentException e) {
            logger.debug("Direct lowercase match failed for: {}", normalized);
        }
        
        // Strategy 2: Try uppercase
        try {
            return NotificationType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.debug("Uppercase match failed for: {}", normalized);
        }
        
        // Strategy 3: Try with first letter capitalized
        try {
            String capitalized = normalized.substring(0, 1).toUpperCase() + normalized.substring(1).toLowerCase();
            return NotificationType.valueOf(capitalized);
        } catch (IllegalArgumentException e) {
            logger.debug("Capitalized match failed for: {}", normalized);
        }
        
        // Strategy 4: Fuzzy matching for common variations
        String fuzzy = normalized.toLowerCase();
        if (fuzzy.contains("mail") || fuzzy.equals("e-mail")) {
            return NotificationType.email;
        }
        if (fuzzy.contains("text") || fuzzy.contains("message")) {
            return NotificationType.sms;
        }
        if (fuzzy.contains("push") || fuzzy.contains("notification")) {
            return NotificationType.app;
        }
        
        // Strategy 5: Default fallback based on common patterns
        if (fuzzy.startsWith("e")) {
            logger.warn("Fuzzy match: treating '{}' as email", normalized);
            return NotificationType.email;
        }
        if (fuzzy.startsWith("s")) {
            logger.warn("Fuzzy match: treating '{}' as sms", normalized);
            return NotificationType.sms;
        }
        if (fuzzy.startsWith("a") || fuzzy.startsWith("p")) {
            logger.warn("Fuzzy match: treating '{}' as app", normalized);
            return NotificationType.app;
        }
        
        // Final fallback: default to email
        logger.warn("Unable to parse notification type: '{}'. Defaulting to email. Valid types are: email, sms, app", normalized);
        return NotificationType.email;
    }
    
    /**
     * Parse notification type with default fallback
     * @param input The input string to parse
     * @param defaultType Fallback type if parsing fails
     * @return Parsed NotificationType or defaultType
     */
    public static NotificationType parseNotificationType(String input, NotificationType defaultType) {
        NotificationType result = parseNotificationType(input);
        if (result == null) {
            logger.warn("Using default notification type '{}' for input '{}'", defaultType, input);
            return defaultType;
        }
        return result;
    }
    
    /**
     * Check if a string represents a valid notification type
     * @param input The input string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidNotificationType(String input) {
        return parseNotificationType(input) != null;
    }
    
    /**
     * Get all valid notification type names
     * @return Array of valid type names
     */
    public static String[] getValidTypes() {
        return new String[]{"email", "sms", "app"};
    }
}
