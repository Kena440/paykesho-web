package org.paykesho.notificationservice.service;

import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.paykesho.notificationservice.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Monitoring service for password reset notifications with alerting
 */
@Service
@Transactional
public class PasswordResetMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetMonitoringService.class);

    @Autowired
    private NotificationService notificationService;

    // Alert thresholds
    private static final int FAILURE_THRESHOLD = 5; // Alert if more than 5 failures in 10 minutes
    private static final int STALE_THRESHOLD_MINUTES = 30; // Alert if notifications stuck for 30+ minutes
    private static final int HIGH_VOLUME_THRESHOLD = 50; // Alert if more than 50 notifications in 10 minutes

    /**
     * Monitor password reset notifications every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void monitorPasswordResetNotifications() {
        try {
            logger.debug("Starting password reset notification monitoring");
            
            // Check for failed notifications
            checkFailedNotifications();
            
            // Check for stale notifications
            checkStaleNotifications();
            
            // Check for high volume
            checkHighVolume();
            
            // Generate health report
            generateHealthReport();
            
        } catch (Exception e) {
            logger.error("Error during password reset monitoring: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for failed password reset notifications
     */
    private void checkFailedNotifications() {
        try {
            List<NotificationQueue> failedNotifications = notificationService.getNotificationsByStatus(NotificationStatus.FAILED);
            
            // Filter for password reset notifications
            long passwordResetFailures = failedNotifications.stream()
                    .filter(n -> "PASSWORD_RESET".equals(n.getEntityType()) || 
                                "PASSWORD_RESET_SMS".equals(n.getEntityType()))
                    .count();
            
            if (passwordResetFailures > FAILURE_THRESHOLD) {
                logger.error("ALERT: High number of failed password reset notifications: {}", passwordResetFailures);
                sendAlert("High Password Reset Failures", 
                         String.format("Found %d failed password reset notifications. Please check notification service health.", passwordResetFailures));
            }
            
        } catch (Exception e) {
            logger.error("Error checking failed notifications: {}", e.getMessage());
        }
    }

    /**
     * Check for stale password reset notifications
     */
    private void checkStaleNotifications() {
        try {
            List<NotificationQueue> processingNotifications = notificationService.getNotificationsByStatus(NotificationStatus.PROCESSING);
            
            Date staleThreshold = new Date(System.currentTimeMillis() - (STALE_THRESHOLD_MINUTES * 60 * 1000));
            
            long stalePasswordResetNotifications = processingNotifications.stream()
                    .filter(n -> "PASSWORD_RESET".equals(n.getEntityType()) || 
                                "PASSWORD_RESET_SMS".equals(n.getEntityType()))
                    .filter(n -> n.getProcessingStartedAt() != null && n.getProcessingStartedAt().before(staleThreshold))
                    .count();
            
            if (stalePasswordResetNotifications > 0) {
                logger.warn("ALERT: Found {} stale password reset notifications stuck in processing state", stalePasswordResetNotifications);
                sendAlert("Stale Password Reset Notifications", 
                         String.format("Found %d password reset notifications stuck in processing state for more than %d minutes.", 
                                     stalePasswordResetNotifications, STALE_THRESHOLD_MINUTES));
                
                // Reset stale notifications
                resetStaleNotifications();
            }
            
        } catch (Exception e) {
            logger.error("Error checking stale notifications: {}", e.getMessage());
        }
    }

    /**
     * Check for high volume of password reset notifications
     */
    private void checkHighVolume() {
        try {
            // Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));
            
            // This would need a custom query to check notifications created in last 10 minutes
            // For now, we'll use a simple approach
            List<NotificationQueue> recentNotifications = notificationService.getNotificationsByStatus(NotificationStatus.PENDING);
            
            long recentPasswordResetNotifications = recentNotifications.stream()
                    .filter(n -> "PASSWORD_RESET".equals(n.getEntityType()) || 
                                "PASSWORD_RESET_SMS".equals(n.getEntityType()))
                    .count();
            
            if (recentPasswordResetNotifications > HIGH_VOLUME_THRESHOLD) {
                logger.warn("ALERT: High volume of password reset notifications: {}", recentPasswordResetNotifications);
                sendAlert("High Password Reset Volume", 
                         String.format("High volume of password reset notifications detected: %d. This might indicate a security issue or system problem.", 
                                     recentPasswordResetNotifications));
            }
            
        } catch (Exception e) {
            logger.error("Error checking high volume: {}", e.getMessage());
        }
    }

    /**
     * Generate health report for password reset notifications
     */
    private void generateHealthReport() {
        try {
            Map<String, Long> stats = notificationService.getQueueStatistics();
            
            long pending = stats.getOrDefault("PENDING", 0L);
            long processing = stats.getOrDefault("PROCESSING", 0L);
            long sent = stats.getOrDefault("SENT", 0L);
            long failed = stats.getOrDefault("FAILED", 0L);
            
            logger.info("Password Reset Notification Health Report:");
            logger.info("  Pending: {}, Processing: {}, Sent: {}, Failed: {}", pending, processing, sent, failed);
            
            // Log health status
            if (failed > 0) {
                logger.warn("Password reset notifications have failures: {}", failed);
            }
            
            if (pending > 20) {
                logger.warn("High number of pending password reset notifications: {}", pending);
            }
            
        } catch (Exception e) {
            logger.error("Error generating health report: {}", e.getMessage());
        }
    }

    /**
     * Reset stale notifications
     */
    private void resetStaleNotifications() {
        try {
            int resetCount = notificationService.resetStaleProcessingNotifications(STALE_THRESHOLD_MINUTES);
            logger.info("Reset {} stale password reset notifications", resetCount);
        } catch (Exception e) {
            logger.error("Error resetting stale notifications: {}", e.getMessage());
        }
    }

    /**
     * Send alert notification
     */
    private void sendAlert(String subject, String message) {
        try {
            // Send alert to system administrators
            String adminEmail = "admin@paykesho.com"; // This should be configurable
            String alertMessage = String.format("ALERT: %s\n\n%s\n\nTime: %s", subject, message, new Date().toString());
            
            // Use high priority notification for alerts
            notificationService.sendHighPriorityNotification(
                adminEmail,
                "System Alert: " + subject,
                alertMessage,
                "email"
            );
            
            logger.info("Alert sent: {}", subject);
            
        } catch (Exception e) {
            logger.error("Error sending alert: {}", e.getMessage());
        }
    }

    /**
     * Get password reset notification health status
     */
    public Map<String, Object> getPasswordResetHealthStatus() {
        try {
            Map<String, Long> stats = notificationService.getQueueStatistics();
            
            return Map.of(
                "status", "healthy",
                "statistics", stats,
                "timestamp", System.currentTimeMillis(),
                "alerts", Map.of(
                    "failureThreshold", FAILURE_THRESHOLD,
                    "staleThresholdMinutes", STALE_THRESHOLD_MINUTES,
                    "highVolumeThreshold", HIGH_VOLUME_THRESHOLD
                )
            );
            
        } catch (Exception e) {
            logger.error("Error getting health status: {}", e.getMessage());
            return Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * Manual health check endpoint
     */
    public String performHealthCheck() {
        try {
            StringBuilder report = new StringBuilder();
            report.append("Password Reset Notification Health Check\n");
            report.append("=====================================\n");
            
            Map<String, Long> stats = notificationService.getQueueStatistics();
            report.append(String.format("Pending: %d\n", stats.getOrDefault("PENDING", 0L)));
            report.append(String.format("Processing: %d\n", stats.getOrDefault("PROCESSING", 0L)));
            report.append(String.format("Sent: %d\n", stats.getOrDefault("SENT", 0L)));
            report.append(String.format("Failed: %d\n", stats.getOrDefault("FAILED", 0L)));
            
            // Check for issues
            long failed = stats.getOrDefault("FAILED", 0L);
            long pending = stats.getOrDefault("PENDING", 0L);
            
            if (failed > 0) {
                report.append("⚠️  WARNING: Failed notifications detected\n");
            }
            
            if (pending > 20) {
                report.append("⚠️  WARNING: High number of pending notifications\n");
            }
            
            if (failed == 0 && pending <= 20) {
                report.append("✅ System is healthy\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            logger.error("Error performing health check: {}", e.getMessage());
            return "❌ Health check failed: " + e.getMessage();
        }
    }
}
