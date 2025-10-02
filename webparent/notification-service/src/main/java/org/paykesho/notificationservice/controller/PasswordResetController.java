package org.paykesho.notificationservice.controller;

import org.commons.entity.NotificationQueue;
import org.paykesho.notificationservice.service.PasswordResetNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for password reset notification management
 */
@RestController
@RequestMapping("/api/notifications/password-reset")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private PasswordResetNotificationService passwordResetNotificationService;

    /**
     * Send password reset email using queue system
     */
    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendPasswordResetEmail(
            @RequestParam String userName,
            @RequestParam String userEmail,
            @RequestParam String resetUrl) {
        
        try {
            logger.info("Received password reset email request for user: {} ({})", userName, userEmail);
            
            NotificationQueue notification = passwordResetNotificationService.sendPasswordResetEmailQueued(
                userName, userEmail, resetUrl);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Password reset email queued successfully",
                "notificationId", notification.getId(),
                "userEmail", userEmail
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending password reset email for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to queue password reset email: " + e.getMessage(),
                "userEmail", userEmail
            ));
        }
    }

    /**
     * Send password reset email with fallback mechanism
     */
    @PostMapping("/send-email-fallback")
    public ResponseEntity<Map<String, Object>> sendPasswordResetEmailWithFallback(
            @RequestParam String userName,
            @RequestParam String userEmail,
            @RequestParam String resetUrl) {
        
        try {
            logger.info("Received password reset email with fallback request for user: {} ({})", userName, userEmail);
            
            boolean success = passwordResetNotificationService.sendPasswordResetEmailWithFallback(
                userName, userEmail, resetUrl);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Password reset email sent successfully" : "Failed to send password reset email",
                "userEmail", userEmail
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending password reset email with fallback for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to send password reset email: " + e.getMessage(),
                "userEmail", userEmail
            ));
        }
    }

    /**
     * Send password reset SMS using queue system
     */
    @PostMapping("/send-sms")
    public ResponseEntity<Map<String, Object>> sendPasswordResetSms(
            @RequestParam String userName,
            @RequestParam String phoneNumber,
            @RequestParam String resetCode) {
        
        try {
            logger.info("Received password reset SMS request for user: {} ({})", userName, phoneNumber);
            
            NotificationQueue notification = passwordResetNotificationService.sendPasswordResetSmsQueued(
                userName, phoneNumber, resetCode);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Password reset SMS queued successfully",
                "notificationId", notification.getId(),
                "phoneNumber", phoneNumber
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending password reset SMS for user {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to queue password reset SMS: " + e.getMessage(),
                "phoneNumber", phoneNumber
            ));
        }
    }

    /**
     * Send multi-channel password reset (email + SMS)
     */
    @PostMapping("/send-multi-channel")
    public ResponseEntity<Map<String, Object>> sendPasswordResetMultiChannel(
            @RequestParam String userName,
            @RequestParam String userEmail,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam String resetUrl,
            @RequestParam String resetCode) {
        
        try {
            logger.info("Received multi-channel password reset request for user: {} ({})", userName, userEmail);
            
            Map<String, Object> results = passwordResetNotificationService.sendPasswordResetMultiChannel(
                userName, userEmail, phoneNumber, resetUrl, resetCode);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error sending multi-channel password reset for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to send multi-channel password reset: " + e.getMessage(),
                "userEmail", userEmail
            ));
        }
    }

    /**
     * Get password reset notifications for a user
     */
    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<NotificationQueue>> getPasswordResetNotifications(@PathVariable String userEmail) {
        try {
            List<NotificationQueue> notifications = passwordResetNotificationService.getPasswordResetNotifications(userEmail);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error getting password reset notifications for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retry failed password reset notification
     */
    @PostMapping("/retry/{notificationId}")
    public ResponseEntity<Map<String, Object>> retryPasswordResetNotification(@PathVariable Integer notificationId) {
        try {
            logger.info("Retrying password reset notification with ID: {}", notificationId);
            
            boolean success = passwordResetNotificationService.retryPasswordResetNotification(notificationId);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Password reset notification queued for retry" : "Failed to retry notification",
                "notificationId", notificationId
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrying password reset notification {}: {}", notificationId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retry notification: " + e.getMessage(),
                "notificationId", notificationId
            ));
        }
    }

    /**
     * Get password reset notification statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getPasswordResetStatistics() {
        try {
            Map<String, Long> stats = passwordResetNotificationService.getPasswordResetStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting password reset statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Health check for password reset notifications
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getPasswordResetHealth() {
        try {
            Map<String, Long> stats = passwordResetNotificationService.getPasswordResetStatistics();
            
            Map<String, Object> health = Map.of(
                "status", "healthy",
                "statistics", stats,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error getting password reset health: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
