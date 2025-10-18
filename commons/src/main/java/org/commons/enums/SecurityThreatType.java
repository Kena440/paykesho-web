package org.commons.enums;

public enum SecurityThreatType {
    XSS_ATTACK("XSS Attack - Script tags detected"),
    SQL_INJECTION("SQL Injection - Dangerous SQL patterns"),
    PATH_TRAVERSAL("Path Traversal - Directory navigation attempt"),
    URL_IN_ADDRESS("URL in Address - Spam/phishing attempt"),
    INVALID_FORMAT("Invalid Format - Data doesn't match required pattern"),
    HTML_INJECTION("HTML Injection - HTML tags detected"),
    JAVASCRIPT_INJECTION("JavaScript Injection - JavaScript code detected"),
    MALICIOUS_FILENAME("Malicious Filename - Dangerous file name pattern"),
    OTHER("Other - Suspicious pattern detected");
    
    private final String description;
    
    SecurityThreatType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

