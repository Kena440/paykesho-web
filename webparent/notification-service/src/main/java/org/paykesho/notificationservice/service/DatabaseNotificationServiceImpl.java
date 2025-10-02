package org.paykesho.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.paykesho.notificationservice.repository.NotificationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of DatabaseNotificationService
 * This service handles database-driven notifications
 */
@Slf4j
@Service
public class DatabaseNotificationServiceImpl implements DatabaseNotificationService {

    @Autowired
    private NotificationRequestRepository notificationRequestRepository;

    /**
     * Create a basic notification request
     */
    @Override
    @Transactional
    public NotificationRequest createNotificationRequest(NotificationType notificationType, 
                                                       String recipientEmail, 
                                                       String subject, 
                                                       String content, 
                                                       String sourceModule) {
        try {
            NotificationRequest request = new NotificationRequest(
                notificationType, recipientEmail, subject, content, sourceModule
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from module {}", 
                    saved.getId(), recipientEmail, sourceModule);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create notification request", e);
        }
    }

    /**
     * Create a high priority notification request
     */
    @Override
    @Transactional
    public NotificationRequest createHighPriorityNotificationRequest(NotificationType notificationType, 
                                                                   String recipientEmail, 
                                                                   String subject, 
                                                                   String content, 
                                                                   String sourceModule) {
        try {
            NotificationRequest request = new NotificationRequest(
                notificationType, recipientEmail, subject, content, sourceModule, 1
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created high priority notification request {} for {} from module {}", 
                    saved.getId(), recipientEmail, sourceModule);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating high priority notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create high priority notification request", e);
        }
    }

    /**
     * Create a scheduled notification request
     */
    @Override
    @Transactional
    public NotificationRequest createScheduledNotificationRequest(NotificationType notificationType, 
                                                                String recipientEmail, 
                                                                String subject, 
                                                                String content, 
                                                                String sourceModule, 
                                                                Date scheduledFor) {
        try {
            NotificationRequest request = new NotificationRequest(
                notificationType, recipientEmail, subject, content, sourceModule, scheduledFor
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created scheduled notification request {} for {} from module {} scheduled for {}", 
                    saved.getId(), recipientEmail, sourceModule, scheduledFor);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating scheduled notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create scheduled notification request", e);
        }
    }

    /**
     * Create notification request with metadata
     */
    @Override
    @Transactional
    public NotificationRequest createNotificationRequestWithMetadata(NotificationType notificationType, 
                                                                   String recipientEmail, 
                                                                   String subject, 
                                                                   String content, 
                                                                   String sourceModule, 
                                                                   String metadata, 
                                                                   String entityReference, 
                                                                   String entityType) {
        try {
            NotificationRequest request = new NotificationRequest(
                notificationType, recipientEmail, subject, content, sourceModule
            );
            
            request.setMetadata(metadata);
            request.setEntityReference(entityReference);
            request.setEntityType(entityType);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request with metadata {} for {} from module {}", 
                    saved.getId(), recipientEmail, sourceModule);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating notification request with metadata: {}", e.getMessage());
            throw new RuntimeException("Failed to create notification request with metadata", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Override
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, 
                                                          String content, 
                                                          String sourceModule) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, sourceModule
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from module {}", 
                    saved.getId(), recipientPhone, sourceModule);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Create password reset notification request
     */
    @Override
    @Transactional
    public NotificationRequest createPasswordResetNotificationRequest(String recipientEmail, 
                                                                    String userName, 
                                                                    String resetUrl, 
                                                                    String sourceModule) {
        try {
            String subject = "Password Reset Request - Paykesho";
            String content = String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\nPaykesho Team",
                userName, resetUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, sourceModule, 1
            );
            request.setEntityType("PASSWORD_RESET");
            request.setMetadata(String.format("{\"userName\":\"%s\",\"resetUrl\":\"%s\"}", userName, resetUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created password reset notification request {} for {} from module {}", 
                    saved.getId(), recipientEmail, sourceModule);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating password reset notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create password reset notification request", e);
        }
    }

    /**
     * Create multi-channel notification request (email + SMS)
     */
    @Override
    @Transactional
    public List<NotificationRequest> createMultiChannelNotificationRequest(String recipientEmail, 
                                                                          String recipientPhone, 
                                                                          String subject, 
                                                                          String content, 
                                                                          String sourceModule) {
        try {
            // Create email notification
            NotificationRequest emailRequest = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, sourceModule
            );
            
            // Create SMS notification
            NotificationRequest smsRequest = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, sourceModule
            );
            smsRequest.setRecipientPhone(recipientPhone);
            
            List<NotificationRequest> saved = notificationRequestRepository.saveAll(List.of(emailRequest, smsRequest));
            log.info("Created multi-channel notification requests {} and {} for {} from module {}", 
                    saved.get(0).getId(), saved.get(1).getId(), recipientEmail, sourceModule);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating multi-channel notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create multi-channel notification request", e);
        }
    }

    /**
     * Get notification request by ID
     */
    @Override
    public Optional<NotificationRequest> getNotificationRequest(Integer id) {
        return notificationRequestRepository.findById(id);
    }

    /**
     * Get notifications by status
     */
    @Override
    public List<NotificationRequest> getNotificationsByStatus(NotificationStatus status) {
        return notificationRequestRepository.findByStatus(status);
    }

    /**
     * Get notifications by source module
     */
    @Override
    public List<NotificationRequest> getNotificationsBySourceModule(String sourceModule) {
        return notificationRequestRepository.findBySourceModule(sourceModule);
    }

    /**
     * Get notifications by recipient
     */
    @Override
    public List<NotificationRequest> getNotificationsByRecipient(String recipientEmail) {
        return notificationRequestRepository.findByRecipientEmail(recipientEmail);
    }

    /**
     * Get pending notifications ready for processing
     */
    @Override
    public List<NotificationRequest> getPendingNotificationsReadyForProcessing() {
        return notificationRequestRepository.findPendingNotificationsReadyForProcessing(
            NotificationStatus.PENDING, new Date()
        );
    }

    /**
     * Get comprehensive notification statistics and health metrics
     */
    @Override
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.count();
            long pending = notificationRequestRepository.countByStatus(NotificationStatus.PENDING);
            long processing = notificationRequestRepository.countByStatus(NotificationStatus.PROCESSING);
            long sent = notificationRequestRepository.countByStatus(NotificationStatus.SENT);
            long failed = notificationRequestRepository.countByStatus(NotificationStatus.FAILED);
            long failedPermanently = notificationRequestRepository.countByStatus(NotificationStatus.FAILED_PERMANENTLY);
            
            // Advanced metrics
            long createdToday = notificationRequestRepository.countNotificationsCreatedToday();
            long sentToday = notificationRequestRepository.countNotificationsSentToday();
            long failedToday = notificationRequestRepository.countNotificationsFailedToday();
            
            // Calculate success rate
            double successRate = (sentToday + failedToday) > 0 ? 
                (double) sentToday / (sentToday + failedToday) * 100 : 0;
            
            // Health status
            String healthStatus = determineHealthStatus(pending, processing, failed, successRate);
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("total", total);
            stats.put("pending", pending);
            stats.put("processing", processing);
            stats.put("sent", sent);
            stats.put("failed", failed);
            stats.put("failedPermanently", failedPermanently);
            stats.put("createdToday", createdToday);
            stats.put("sentToday", sentToday);
            stats.put("failedToday", failedToday);
            stats.put("successRate", String.format("%.2f%%", successRate));
            stats.put("healthStatus", healthStatus);
            stats.put("timestamp", new Date());
            return stats;
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * Determine system health status based on metrics
     */
    private String determineHealthStatus(long pending, long processing, long failed, double successRate) {
        if (successRate < 50) return "CRITICAL";
        if (pending > 1000 || processing > 100) return "WARNING";
        if (failed > 100) return "DEGRADED";
        if (successRate >= 95 && pending < 100) return "HEALTHY";
        return "STABLE";
    }

    /**
     * Update notification request status
     */
    @Override
    @Transactional
    public void updateNotificationStatus(Integer id, NotificationStatus status) {
        try {
            Optional<NotificationRequest> optional = notificationRequestRepository.findById(id);
            if (optional.isPresent()) {
                NotificationRequest request = optional.get();
                request.setStatus(status);
                notificationRequestRepository.save(request);
                log.info("Updated notification request {} status to {}", id, status);
            }
        } catch (Exception e) {
            log.error("Error updating notification status: {}", e.getMessage());
        }
    }

    /**
     * Mark notification as failed with smart retry logic
     */
    @Override
    @Transactional
    public void markNotificationAsFailed(Integer id, String errorMessage) {
        try {
            Optional<NotificationRequest> optional = notificationRequestRepository.findById(id);
            if (optional.isPresent()) {
                NotificationRequest request = optional.get();
                
                // Smart retry logic based on error type
                if (shouldRetry(errorMessage, request.getAttemptCount())) {
                    int retryDelay = calculateRetryDelay(request.getAttemptCount());
                    request.markAsFailed(errorMessage);
                    request.setScheduledFor(new Date(System.currentTimeMillis() + retryDelay));
                    log.info("Scheduled retry for notification request {} in {}ms (attempt {})", 
                            id, retryDelay, request.getAttemptCount());
                } else {
                    request.markAsFailed(errorMessage);
                    log.warn("Permanently failed notification request {}: {}", id, errorMessage);
                }
                
                notificationRequestRepository.save(request);
            }
        } catch (Exception e) {
            log.error("Error marking notification as failed: {}", e.getMessage());
        }
    }
    
    /**
     * Determine if notification should be retried based on error type and attempt count
     */
    private boolean shouldRetry(String errorMessage, int attemptCount) {
        if (attemptCount >= 3) return false; // Max 3 attempts
        
        // Don't retry for certain permanent errors
        String lowerError = errorMessage.toLowerCase();
        if (lowerError.contains("invalid email") || 
            lowerError.contains("user not found") ||
            lowerError.contains("blocked") ||
            lowerError.contains("bounced")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate retry delay with exponential backoff
     */
    private int calculateRetryDelay(int attemptCount) {
        // Exponential backoff: 1min, 5min, 15min
        int[] delays = {60000, 300000, 900000}; // in milliseconds
        return attemptCount < delays.length ? delays[attemptCount] : delays[delays.length - 1];
    }

    /**
     * Mark notification as sent
     */
    @Override
    @Transactional
    public void markNotificationAsSent(Integer id) {
        try {
            Optional<NotificationRequest> optional = notificationRequestRepository.findById(id);
            if (optional.isPresent()) {
                NotificationRequest request = optional.get();
                request.markAsSent();
                notificationRequestRepository.save(request);
                log.info("Marked notification request {} as sent", id);
            }
        } catch (Exception e) {
            log.error("Error marking notification as sent: {}", e.getMessage());
        }
    }
}
