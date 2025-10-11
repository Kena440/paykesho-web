package org.paykesho.notificationservice.controller;

import org.commons.dto.NotificationQueueDto;
import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.commons.utils.NotificationTypeUtils;
import org.paykesho.notificationservice.service.NotificationProcessorService;
import org.paykesho.notificationservice.service.NotificationQueueService;
import org.paykesho.notificationservice.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for notification queue management
 */
@RestController
@RequestMapping("/api/notifications/queue")
public class NotificationQueueController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationQueueController.class);

    @Autowired
    private NotificationQueueService notificationQueueService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationProcessorService notificationProcessorService;

    /**
     * Add a new notification to the queue
     */
    @PostMapping("/add")
    public ResponseEntity<NotificationQueueDto> addNotification(
            @RequestParam String notificationType,
            @RequestParam String recipientEmail,
            @RequestParam String subject,
            @RequestParam String content) {
        
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            NotificationQueue notification = notificationQueueService.addNotification(type, recipientEmail, subject, content);
            return ResponseEntity.ok(new NotificationQueueDto(notification));
        } catch (Exception e) {
            logger.error("Error adding notification to queue: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add a high priority notification to the queue
     */
    @PostMapping("/add-high-priority")
    public ResponseEntity<NotificationQueueDto> addHighPriorityNotification(
            @RequestParam String notificationType,
            @RequestParam String recipientEmail,
            @RequestParam String subject,
            @RequestParam String content) {
        
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            NotificationQueue notification = notificationQueueService.addHighPriorityNotification(type, recipientEmail, subject, content);
            return ResponseEntity.ok(new NotificationQueueDto(notification));
        } catch (Exception e) {
            logger.error("Error adding high priority notification to queue: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add a scheduled notification to the queue
     */
    @PostMapping("/add-scheduled")
    public ResponseEntity<NotificationQueueDto> addScheduledNotification(
            @RequestParam String notificationType,
            @RequestParam String recipientEmail,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam Long scheduledForTimestamp) {
        
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            Date scheduledFor = new Date(scheduledForTimestamp);
            NotificationQueue notification = notificationQueueService.addScheduledNotification(type, recipientEmail, subject, content, scheduledFor);
            return ResponseEntity.ok(new NotificationQueueDto(notification));
        } catch (Exception e) {
            logger.error("Error adding scheduled notification to queue: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get notifications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NotificationQueueDto>> getNotificationsByStatus(@PathVariable String status) {
        try {
            NotificationStatus notificationStatus = NotificationStatus.valueOf(status.toUpperCase());
            List<NotificationQueue> notifications = notificationQueueService.getNotificationsByStatus(notificationStatus);
            List<NotificationQueueDto> dtos = notifications.stream()
                    .map(NotificationQueueDto::new)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error getting notifications by status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get notifications by status with pagination
     */
    @GetMapping("/status/{status}/paged")
    public ResponseEntity<Page<NotificationQueueDto>> getNotificationsByStatusPaged(
            @PathVariable String status, Pageable pageable) {
        try {
            NotificationStatus notificationStatus = NotificationStatus.valueOf(status.toUpperCase());
            Page<NotificationQueue> notifications = notificationQueueService.getNotificationsByStatus(notificationStatus, pageable);
            Page<NotificationQueueDto> dtos = notifications.map(NotificationQueueDto::new);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error getting paginated notifications by status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get notifications by recipient
     */
    @GetMapping("/recipient/{recipientEmail}")
    public ResponseEntity<List<NotificationQueueDto>> getNotificationsByRecipient(@PathVariable String recipientEmail) {
        try {
            List<NotificationQueue> notifications = notificationQueueService.getNotificationsByRecipient(recipientEmail);
            List<NotificationQueueDto> dtos = notifications.stream()
                    .map(NotificationQueueDto::new)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error getting notifications by recipient: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get notification by UUID
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<NotificationQueueDto> getNotificationById(@PathVariable String uuid) {
        try {
            NotificationQueue notification = notificationQueueService.getNotificationByUuid(uuid);
            return ResponseEntity.ok(new NotificationQueueDto(notification));
        } catch (RuntimeException e) {
            logger.error("Notification not found with UUID: {}", uuid);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting notification by UUID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancel a notification
     */
    @PostMapping("/{uuid}/cancel")
    public ResponseEntity<String> cancelNotification(@PathVariable String uuid) {
        try {
            NotificationQueue notification = notificationQueueService.getNotificationByUuid(uuid);
            notificationQueueService.cancelNotification(notification.getId());
            return ResponseEntity.ok("Notification cancelled successfully");
        } catch (Exception e) {
            logger.error("Error cancelling notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error cancelling notification: " + e.getMessage());
        }
    }

    /**
     * Retry a failed notification
     */
    @PostMapping("/{uuid}/retry")
    public ResponseEntity<String> retryNotification(@PathVariable String uuid) {
        try {
            NotificationQueue notification = notificationQueueService.getNotificationByUuid(uuid);
            notificationQueueService.retryNotification(notification.getId());
            return ResponseEntity.ok("Notification queued for retry");
        } catch (Exception e) {
            logger.error("Error retrying notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error retrying notification: " + e.getMessage());
        }
    }

    /**
     * Get queue statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getQueueStatistics() {
        try {
            Map<String, Long> stats = notificationQueueService.getNotificationStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting queue statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get queue health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> getQueueHealth() {
        try {
            String health = notificationService.getQueueHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Error getting queue health: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error retrieving queue health: " + e.getMessage());
        }
    }

    /**
     * Process a notification immediately
     */
    @PostMapping("/{uuid}/process")
    public ResponseEntity<String> processNotificationImmediately(@PathVariable String uuid) {
        try {
            NotificationQueue notification = notificationQueueService.getNotificationByUuid(uuid);
            boolean success = notificationProcessorService.processNotificationImmediately(notification.getId());
            if (success) {
                return ResponseEntity.ok("Notification processed successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to process notification");
            }
        } catch (Exception e) {
            logger.error("Error processing notification immediately: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error processing notification: " + e.getMessage());
        }
    }

    /**
     * Get processing statistics
     */
    @GetMapping("/processing/statistics")
    public ResponseEntity<String> getProcessingStatistics() {
        try {
            String stats = notificationProcessorService.getProcessingStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting processing statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error retrieving processing statistics: " + e.getMessage());
        }
    }

    /**
     * Reset stale processing notifications
     */
    @PostMapping("/reset-stale")
    public ResponseEntity<String> resetStaleNotifications(@RequestParam(defaultValue = "30") int staleMinutes) {
        try {
            int resetCount = notificationQueueService.resetStaleProcessingNotifications(staleMinutes);
            return ResponseEntity.ok("Reset " + resetCount + " stale notifications");
        } catch (Exception e) {
            logger.error("Error resetting stale notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error resetting stale notifications: " + e.getMessage());
        }
    }

    /**
     * Clean up old notifications
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldNotifications(@RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            int deletedCount = notificationQueueService.cleanupOldNotifications(daysToKeep);
            return ResponseEntity.ok("Cleaned up " + deletedCount + " old notifications");
        } catch (Exception e) {
            logger.error("Error cleaning up old notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error cleaning up old notifications: " + e.getMessage());
        }
    }
}
