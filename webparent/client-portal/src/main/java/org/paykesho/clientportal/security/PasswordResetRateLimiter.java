package org.paykesho.clientportal.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiter for password reset requests to prevent email enumeration and brute force attacks
 * 
 * Security Features:
 * - Per-IP rate limiting
 * - Per-email rate limiting  
 * - Progressive delays for repeated attempts
 * - Configurable limits and time windows
 */
@Slf4j
@Component
public class PasswordResetRateLimiter {

    // Rate limiting configuration
    private static final int MAX_ATTEMPTS_PER_IP_PER_HOUR = 5;
    private static final int MAX_ATTEMPTS_PER_EMAIL_PER_HOUR = 3;
    private static final int MAX_ATTEMPTS_PER_IP_PER_DAY = 15;
    private static final int MAX_ATTEMPTS_PER_EMAIL_PER_DAY = 5;
    
    // Progressive delay configuration (in milliseconds)
    private static final long[] PROGRESSIVE_DELAYS = {0, 2000, 5000, 10000, 30000, 60000}; // 0s, 2s, 5s, 10s, 30s, 60s
    
    // Storage for rate limiting data
    private final ConcurrentMap<String, RateLimitData> ipAttempts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RateLimitData> emailAttempts = new ConcurrentHashMap<>();
    
    /**
     * Check if password reset request is allowed and apply rate limiting
     * @param clientIp The client's IP address
     * @param email The email address requesting password reset
     * @return RateLimitResult indicating if request is allowed and any required delay
     */
    public RateLimitResult checkRateLimit(String clientIp, String email) {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up old entries
        cleanupExpiredEntries(now);
        
        // Check IP-based rate limits
        RateLimitResult ipResult = checkIpRateLimit(clientIp, now);
        if (!ipResult.isAllowed()) {
            log.warn("IP rate limit exceeded for {}: {}", clientIp, ipResult.getReason());
            return ipResult;
        }
        
        // Check email-based rate limits
        RateLimitResult emailResult = checkEmailRateLimit(email, now);
        if (!emailResult.isAllowed()) {
            log.warn("Email rate limit exceeded for {}: {}", email, emailResult.getReason());
            return emailResult;
        }
        
        // Both checks passed - record the attempt
        recordAttempt(clientIp, email, now);
        
        // Calculate progressive delay
        long delay = calculateProgressiveDelay(clientIp, email);
        
        log.info("Password reset request allowed for IP: {}, Email: {}, Delay: {}ms", clientIp, email, delay);
        return new RateLimitResult(true, "Request allowed", delay);
    }
    
    /**
     * Check IP-based rate limits
     */
    private RateLimitResult checkIpRateLimit(String ip, LocalDateTime now) {
        RateLimitData ipData = ipAttempts.get(ip);
        
        if (ipData == null) {
            return new RateLimitResult(true, "First request from IP", 0);
        }
        
        // Check hourly limit
        long hourlyAttempts = ipData.getAttemptsInLastHour(now);
        if (hourlyAttempts >= MAX_ATTEMPTS_PER_IP_PER_HOUR) {
            return new RateLimitResult(false, "IP hourly limit exceeded", 0);
        }
        
        // Check daily limit
        long dailyAttempts = ipData.getAttemptsInLastDay(now);
        if (dailyAttempts >= MAX_ATTEMPTS_PER_IP_PER_DAY) {
            return new RateLimitResult(false, "IP daily limit exceeded", 0);
        }
        
        return new RateLimitResult(true, "IP limits OK", 0);
    }
    
    /**
     * Check email-based rate limits
     */
    private RateLimitResult checkEmailRateLimit(String email, LocalDateTime now) {
        RateLimitData emailData = emailAttempts.get(email);
        
        if (emailData == null) {
            return new RateLimitResult(true, "First request for email", 0);
        }
        
        // Check hourly limit
        long hourlyAttempts = emailData.getAttemptsInLastHour(now);
        if (hourlyAttempts >= MAX_ATTEMPTS_PER_EMAIL_PER_HOUR) {
            return new RateLimitResult(false, "Email hourly limit exceeded", 0);
        }
        
        // Check daily limit
        long dailyAttempts = emailData.getAttemptsInLastDay(now);
        if (dailyAttempts >= MAX_ATTEMPTS_PER_EMAIL_PER_DAY) {
            return new RateLimitResult(false, "Email daily limit exceeded", 0);
        }
        
        return new RateLimitResult(true, "Email limits OK", 0);
    }
    
    /**
     * Record a password reset attempt
     */
    private void recordAttempt(String ip, String email, LocalDateTime now) {
        // Record IP attempt
        ipAttempts.compute(ip, (key, existing) -> {
            if (existing == null) {
                return new RateLimitData(now);
            } else {
                existing.addAttempt(now);
                return existing;
            }
        });
        
        // Record email attempt
        emailAttempts.compute(email, (key, existing) -> {
            if (existing == null) {
                return new RateLimitData(now);
            } else {
                existing.addAttempt(now);
                return existing;
            }
        });
    }
    
    /**
     * Calculate progressive delay based on previous attempts
     */
    private long calculateProgressiveDelay(String ip, String email) {
        RateLimitData ipData = ipAttempts.get(ip);
        RateLimitData emailData = emailAttempts.get(email);
        
        int ipAttempts = ipData != null ? ipData.getTotalAttempts() : 0;
        int emailAttempts = emailData != null ? emailData.getTotalAttempts() : 0;
        
        // Use the higher of IP or email attempts for delay calculation
        int maxAttempts = Math.max(ipAttempts, emailAttempts);
        
        if (maxAttempts < PROGRESSIVE_DELAYS.length) {
            return PROGRESSIVE_DELAYS[maxAttempts];
        } else {
            // Cap at maximum delay
            return PROGRESSIVE_DELAYS[PROGRESSIVE_DELAYS.length - 1];
        }
    }
    
    /**
     * Clean up expired entries to prevent memory leaks
     */
    private void cleanupExpiredEntries(LocalDateTime now) {
        // Clean up IP entries older than 24 hours
        ipAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(now.minus(24, ChronoUnit.HOURS)));
        
        // Clean up email entries older than 24 hours
        emailAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(now.minus(24, ChronoUnit.HOURS)));
    }
    
    /**
     * Rate limit result containing decision and delay information
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final String reason;
        private final long delayMs;
        
        public RateLimitResult(boolean allowed, String reason, long delayMs) {
            this.allowed = allowed;
            this.reason = reason;
            this.delayMs = delayMs;
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        public long getDelayMs() { return delayMs; }
    }
    
    /**
     * Data structure to track rate limiting information
     */
    private static class RateLimitData {
        private final ConcurrentHashMap<LocalDateTime, Integer> attempts = new ConcurrentHashMap<>();
        private LocalDateTime lastAttempt;
        
        public RateLimitData(LocalDateTime firstAttempt) {
            this.lastAttempt = firstAttempt;
            addAttempt(firstAttempt);
        }
        
        public void addAttempt(LocalDateTime attemptTime) {
            attempts.put(attemptTime, attempts.getOrDefault(attemptTime, 0) + 1);
            lastAttempt = attemptTime;
        }
        
        public long getAttemptsInLastHour(LocalDateTime now) {
            LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
            return attempts.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(oneHourAgo))
                .mapToInt(entry -> entry.getValue())
                .sum();
        }
        
        public long getAttemptsInLastDay(LocalDateTime now) {
            LocalDateTime oneDayAgo = now.minus(1, ChronoUnit.DAYS);
            return attempts.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(oneDayAgo))
                .mapToInt(entry -> entry.getValue())
                .sum();
        }
        
        public int getTotalAttempts() {
            return attempts.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }
    }
}
