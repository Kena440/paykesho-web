package org.commons.utils;

import org.commons.enums.FileType;

/**
 * Utility class for handling file access with fallback support
 */
public class FileAccessUtil {
    
    /**
     * Get file URL with fallback to placeholder if URL is invalid
     */
    public static String getFileUrlOrPlaceholder(String fileUrl, FileType fileType, String baseUrl) {
        if (fileUrl == null || fileUrl.isEmpty() || fileUrl.equals("null")) {
            return getPlaceholderUrl(fileType, baseUrl);
        }
        return fileUrl;
    }
    
    /**
     * Get placeholder URL for a specific file type
     */
    public static String getPlaceholderUrl(FileType fileType, String baseUrl) {
        if (fileType == null) {
            return baseUrl + "/placeholders/file";
        }
        return baseUrl + "/placeholders/" + fileType.name().toLowerCase();
    }
    
    /**
     * Check if a file URL is a placeholder
     */
    public static boolean isPlaceholder(String fileUrl) {
        return fileUrl != null && fileUrl.contains("/placeholders/");
    }
    
    /**
     * Get a safe file URL with error handling
     */
    public static String getSafeFileUrl(String fileUrl, FileType defaultType, String baseUrl) {
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                return getPlaceholderUrl(defaultType, baseUrl);
            }
            
            // Check if URL is accessible (basic validation)
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                return fileUrl;
            }
            
            // If it's a relative path, construct full URL
            if (fileUrl.startsWith("/")) {
                return baseUrl + fileUrl;
            }
            
            return fileUrl;
        } catch (Exception e) {
            return getPlaceholderUrl(defaultType, baseUrl);
        }
    }
}

