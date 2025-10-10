package org.paykesho.notificationservice.service;

import org.commons.entity.NotificationQueue;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.commons.exception.BusinessLogicException;
import org.paykesho.notificationservice.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Scheduled service for processing notification queue with integration to existing notification system
 */
@Service
@Transactional
public class NotificationProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessorService.class);

    @Autowired
    private NotificationQueueService notificationQueueService;
    
    @Autowired
    private NotificationService legacyNotificationService;
    
    @Autowired
    private DatabaseNotificationService databaseNotificationService;
    
    @Autowired
    private org.paykesho.notificationservice.utils.EmailTemplateService emailTemplateService;

    // Hardcoded configuration values
    private static final int RATE_LIMIT_DELAY_MS = 100;
    private static final int STALE_MINUTES = 30;
    private static final int DAYS_TO_KEEP = 30;

    /**
     * Process database-driven notification requests with intelligent batching
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds for near real-time processing
    public void processDatabaseNotificationRequests() {
        LOGGER.debug("Starting intelligent database notification request processing");
        
        try {
            List<NotificationRequest> pendingRequests = databaseNotificationService.getPendingNotificationsReadyForProcessing();
            
            if (pendingRequests.isEmpty()) {
                LOGGER.debug("No pending database notification requests found");
                return;
            }
            
            LOGGER.info("Processing {} database notification requests with intelligent batching", pendingRequests.size());
            
            // Intelligent batching: Group by type and priority
            Map<String, List<NotificationRequest>> batchedRequests = pendingRequests.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    req -> req.getNotificationType().toString() + "_" + req.getPriority()
                ));
            
            // Process high priority first, then by type
            batchedRequests.entrySet().stream()
                .sorted((e1, e2) -> {
                    String[] parts1 = e1.getKey().split("_");
                    String[] parts2 = e2.getKey().split("_");
                    int priority1 = Integer.parseInt(parts1[1]);
                    int priority2 = Integer.parseInt(parts2[1]);
                    return Integer.compare(priority1, priority2);
                })
                .forEach(entry -> {
                    List<NotificationRequest> batch = entry.getValue();
                    processBatchIntelligently(batch);
                });
            
        } catch (Exception e) {
            LOGGER.error("Error in database notification request processing: {}", e.getMessage());
        }
    }
    
    /**
     * Process a batch of notifications intelligently
     */
    private void processBatchIntelligently(List<NotificationRequest> batch) {
        if (batch.isEmpty()) return;
        
        String batchType = batch.get(0).getNotificationType().toString();
        LOGGER.info("Processing batch of {} {} notifications", batch.size(), batchType);
        
        // Process in parallel for same type
        batch.parallelStream().forEach(request -> {
            try {
                processDatabaseNotificationRequest(request);
                
                // Adaptive rate limiting based on batch size
                int delay = Math.max(50, RATE_LIMIT_DELAY_MS / batch.size());
                Thread.sleep(delay);
            } catch (Exception e) {
                LOGGER.error("Error processing notification request {}: {}", request.getId(), e.getMessage());
            }
        });
    }

    /**
     * Process pending notifications every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void processPendingNotifications() {
        LOGGER.debug("Starting scheduled notification processing");
        
        try {
            List<NotificationQueue> pendingNotifications = notificationQueueService.getPendingNotifications();
            
            if (pendingNotifications.isEmpty()) {
                LOGGER.debug("No pending notifications to process");
                return;
            }
            
            LOGGER.info("Processing {} pending notifications", pendingNotifications.size());
            
            // Process notifications in batches
            processNotificationsBatch(pendingNotifications);
            
        } catch (Exception e) {
            LOGGER.error("Error during scheduled notification processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Process retryable failed notifications every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processRetryableNotifications() {
        LOGGER.debug("Starting retryable notification processing");
        
        try {
            List<NotificationQueue> retryableNotifications = notificationQueueService.getRetryableNotifications();
            
            if (retryableNotifications.isEmpty()) {
                LOGGER.debug("No retryable notifications to process");
                return;
            }
            
            LOGGER.info("Retrying {} failed notifications", retryableNotifications.size());
            
            // Process retry notifications
            for (NotificationQueue notification : retryableNotifications) {
                try {
                    // Increment retry count
                    notificationQueueService.retryNotification(notification.getId());
                    
                    // Process the notification
                    processSingleNotification(notification);
                    
                } catch (BusinessLogicException e) {
                    LOGGER.warn("Cannot retry notification {}: {}", notification.getId(), e.getMessage());
                } catch (Exception e) {
                    LOGGER.error("Error retrying notification {}: {}", notification.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during retryable notification processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old notifications daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        LOGGER.info("Starting daily cleanup of old notifications");
        
        try {
            int deletedCount = notificationQueueService.cleanupOldNotifications(DAYS_TO_KEEP);
            
            if (deletedCount > 0) {
                LOGGER.info("Cleaned up {} old notifications", deletedCount);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during notification cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset stale processing notifications every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void resetStaleProcessingNotifications() {
        LOGGER.debug("Checking for stale processing notifications");
        
        try {
            int resetCount = notificationQueueService.resetStaleProcessingNotifications(STALE_MINUTES);
            
            if (resetCount > 0) {
                LOGGER.info("Reset {} stale processing notifications", resetCount);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error resetting stale notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Process notifications in batches with rate limiting
     */
    @Async("notificationTaskExecutor")
    private CompletableFuture<Void> processNotificationsBatch(List<NotificationQueue> notifications) {
        int processed = 0;
        int delay = 0;
        
        for (NotificationQueue notification : notifications) {
            try {
                // Apply rate limiting delay
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                
                // Process single notification
                processSingleNotification(notification);
                
                processed++;
                
                // Increase delay for rate limiting (exponential backoff)
                delay = Math.min(RATE_LIMIT_DELAY_MS * processed, 1000); // Max 1 second delay
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Notification processing interrupted");
                break;
                
            } catch (Exception e) {
                LOGGER.error("Error processing notification {}: {}", notification.getId(), e.getMessage());
                
                // Mark as failed
                notificationQueueService.markNotificationAsFailed(notification.getId(), e.getMessage());
            }
        }
        
        LOGGER.info("Processed {} notifications in batch", processed);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process a single database notification request
     */
    private void processDatabaseNotificationRequest(NotificationRequest request) {
        try {
            LOGGER.debug("Processing database notification request {} of type {} for {}", 
                        request.getId(), request.getNotificationType(), request.getRecipientEmail());
            
            // Mark as processing
            request.markAsProcessing();
            databaseNotificationService.updateNotificationStatus(request.getId(), NotificationStatus.PROCESSING);
            
            // Use existing notification service to send the notification
            boolean success = false;
            String errorMessage = null;
            
            try {
                if (request.getNotificationType() == NotificationType.email) {
                    // Generate proper HTML email content using templates
                    String emailContent = generateEmailContent(request);
                    
                    success = legacyNotificationService.sendEmail(
                        request.getRecipientEmail(), 
                        request.getSubject(), 
                        emailContent
                    );
                } else if (request.getNotificationType() == NotificationType.sms) {
                    success = legacyNotificationService.sendSms(
                        request.getRecipientPhone() != null ? request.getRecipientPhone() : request.getRecipientEmail(), 
                        request.getContent()
                    );
                    // Set error message if SMS failed
                    if (!success && errorMessage == null) {
                        errorMessage = "SMS API returned failed status";
                    }
                } else {
                    LOGGER.warn("Unsupported notification type: {}", request.getNotificationType());
                    errorMessage = "Unsupported notification type: " + request.getNotificationType();
                }
                
            } catch (Exception e) {
                LOGGER.error("Error sending database notification request {}: {}", request.getId(), e.getMessage());
                success = false;
                errorMessage = e.getMessage();
            }
            
            // Update notification result
            if (success) {
                databaseNotificationService.markNotificationAsSent(request.getId());
                LOGGER.info("Successfully processed database notification request {}", request.getId());
            } else {
                databaseNotificationService.markNotificationAsFailed(request.getId(), errorMessage);
                LOGGER.warn("Failed to process database notification request {}: {}", request.getId(), errorMessage);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing database notification request {}: {}", request.getId(), e.getMessage());
            databaseNotificationService.markNotificationAsFailed(request.getId(), e.getMessage());
        }
    }
    
    /**
     * Generate email content using templates based on entity type
     * This ensures all emails use uniform professional designs
     */
    private String generateEmailContent(NotificationRequest request) {
        try {
            // Extract metadata if available
            String metadata = request.getMetadata();
            String entityType = request.getEntityType();
            String content = request.getContent();
            
            // If content is marked as template-generated, force template usage
            boolean forceTemplate = "TEMPLATE_GENERATED".equals(content);
            
            // Use professional templates based on entity type
            if ("PASSWORD_RESET".equals(entityType) || 
                "MERCHANT_PASSWORD_RESET".equals(entityType) || 
                "ORGANIZATION_PASSWORD_RESET".equals(entityType) || 
                "CLIENT_PASSWORD_RESET".equals(entityType)) {
                // Extract userName and resetUrl from metadata
                String userName = extractFromMetadata(metadata, "userName");
                String resetUrl = extractFromMetadata(metadata, "resetUrl");
                
                if (userName != null && resetUrl != null) {
                    LOGGER.info("Generating password reset email using HTML template for {}", request.getRecipientEmail());
                    return emailTemplateService.generatePasswordResetEmail(userName, request.getRecipientEmail(), resetUrl);
                } else {
                    LOGGER.warn("Missing userName or resetUrl for password reset email {}: userName={}, resetUrl={}", 
                               request.getId(), userName, resetUrl);
                }
            } else if ("VOUCHER".equals(entityType)) {
                // Extract voucher details from metadata
                String clientName = extractFromMetadata(metadata, "clientName");
                String voucherReference = request.getEntityReference();
                String actionType = extractFromMetadata(metadata, "actionType");
                String message = extractFromMetadata(metadata, "message");
                String amountStr = extractFromMetadata(metadata, "amount");
                
                if (clientName != null && voucherReference != null && actionType != null && amountStr != null) {
                    LOGGER.info("Generating voucher notification email using HTML template for {}", request.getRecipientEmail());
                    java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
                    return emailTemplateService.generateVoucherNotificationEmail(
                        clientName, request.getRecipientEmail(), voucherReference, actionType, message, amount
                    );
                }
            } else if ("TRANSACTION".equals(entityType)) {
                // Extract transaction details from metadata
                String transactionReference = request.getEntityReference();
                String merchantName = extractFromMetadata(metadata, "merchantName");
                String clientName = extractFromMetadata(metadata, "clientName");
                String amountStr = extractFromMetadata(metadata, "amount");
                
                if (transactionReference != null && merchantName != null && clientName != null && amountStr != null) {
                    LOGGER.info("Generating transaction notification email using HTML template for {}", request.getRecipientEmail());
                    java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
                    return emailTemplateService.generateTransactionNotificationEmail(
                        clientName, request.getRecipientEmail(), transactionReference, merchantName, amount
                    );
                }
            }
            
            // Fall back to original content if template generation fails or unsupported type
            if (forceTemplate) {
                LOGGER.error("Template generation failed for notification {} (entity type: {}) - cannot use fallback", 
                           request.getId(), entityType);
                // Generate a basic HTML email instead of plain text
                return generateBasicHtmlEmail(request);
            } else {
                LOGGER.info("Using original content for notification {} (entity type: {})", request.getId(), entityType);
                return content;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error generating email content from template: {}", e.getMessage());
            // Generate a basic HTML email instead of plain text fallback
            return generateBasicHtmlEmail(request);
        }
    }
    
    /**
     * Generate a basic HTML email when template generation fails
     * This ensures emails are never sent as plain text
     */
    private String generateBasicHtmlEmail(NotificationRequest request) {
        try {
            String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>%s</title></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #007bff;'>%s</h2>" +
                "<div style='white-space: pre-line;'>%s</div>" +
                "<hr style='margin: 20px 0; border: none; border-top: 1px solid #eee;'>" +
                "<p style='color: #666; font-size: 12px;'>Best regards,<br>Paykesho Team</p>" +
                "</div></body></html>",
                request.getSubject(),
                request.getSubject(),
                request.getContent().replace("<", "&lt;").replace(">", "&gt;")
            );
            LOGGER.info("Generated basic HTML email for notification {}", request.getId());
            return htmlContent;
        } catch (Exception e) {
            LOGGER.error("Error generating basic HTML email: {}", e.getMessage());
            // Last resort - return original content
            return request.getContent();
        }
    }
    
    /**
     * Extract value from JSON-like metadata string
     */
    private String extractFromMetadata(String metadata, String key) {
        if (metadata == null || key == null) {
            return null;
        }
        
        try {
            // Simple JSON parsing for metadata like: {"userName":"John","resetUrl":"http://..."}
            String searchKey = "\"" + key + "\":\"";
            int startIndex = metadata.indexOf(searchKey);
            if (startIndex == -1) {
                return null;
            }
            
            startIndex += searchKey.length();
            int endIndex = metadata.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return null;
            }
            
            return metadata.substring(startIndex, endIndex);
        } catch (Exception e) {
            LOGGER.error("Error extracting {} from metadata: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Process a single notification using the existing notification service
     */
    private void processSingleNotification(NotificationQueue notification) {
        try {
            LOGGER.debug("Processing notification {} of type {} for {}", 
                        notification.getId(), notification.getNotificationType(), notification.getRecipientEmail());
            
            // Validate notification can be processed
            validateNotificationForProcessing(notification);
            
            // Mark as processing
            notificationQueueService.updateNotificationStatus(notification.getId(), NotificationStatus.PROCESSING);
            
            // Use existing notification service to send the notification
            boolean success = false;
            String errorMessage = null;
            
            try {
                if (notification.getNotificationType() == NotificationType.email) {
                    success = legacyNotificationService.sendEmail(
                        notification.getRecipientEmail(), 
                        notification.getSubject(), 
                        notification.getContent()
                    );
                } else if (notification.getNotificationType() == NotificationType.sms) {
                    success = legacyNotificationService.sendSms(
                        notification.getRecipientEmail(), 
                        notification.getContent()
                    );
                    // Set error message if SMS failed
                    if (!success && errorMessage == null) {
                        errorMessage = "SMS API returned failed status";
                    }
                } else {
                    LOGGER.warn("Unsupported notification type: {}", notification.getNotificationType());
                    errorMessage = "Unsupported notification type: " + notification.getNotificationType();
                }
                
            } catch (Exception e) {
                LOGGER.error("Error sending notification {}: {}", notification.getId(), e.getMessage());
                success = false;
                errorMessage = e.getMessage();
            }
            
            // Update notification result
            if (success) {
                notificationQueueService.updateNotificationStatus(notification.getId(), NotificationStatus.SENT);
                LOGGER.info("Successfully sent notification {} to {}", notification.getId(), notification.getRecipientEmail());
            } else {
                notificationQueueService.markNotificationAsFailed(notification.getId(), errorMessage);
                LOGGER.warn("Failed to send notification {} to {}: {}", notification.getId(), notification.getRecipientEmail(), errorMessage);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to process notification {}: {}", notification.getId(), e.getMessage());
            notificationQueueService.markNotificationAsFailed(notification.getId(), e.getMessage());
        }
    }

    /**
     * Validate that notification can be processed
     */
    private void validateNotificationForProcessing(NotificationQueue notification) {
        // Check if notification is in correct state
        if (notification.getStatus() != NotificationStatus.PENDING && 
            notification.getStatus() != NotificationStatus.FAILED) {
            throw new BusinessLogicException(
                String.format("Notification %d is in %s status and cannot be processed", 
                            notification.getId(), notification.getStatus()),
                "NOTIFICATION_NOT_PROCESSABLE"
            );
        }
        
        // Check if notification is scheduled for future
        if (notification.isScheduled() && notification.getScheduledFor().after(new Date())) {
            throw new BusinessLogicException(
                String.format("Notification %d is scheduled for future processing", notification.getId()),
                "NOTIFICATION_SCHEDULED_FUTURE"
            );
        }
        
        // Check retry limits for failed notifications
        if (notification.getStatus() == NotificationStatus.FAILED && !notification.canRetry()) {
            throw new BusinessLogicException(
                String.format("Notification %d has exceeded maximum retry attempts", notification.getId()),
                "NOTIFICATION_MAX_RETRIES_EXCEEDED"
            );
        }
    }

    /**
     * Get processing statistics
     */
    @Transactional(readOnly = true)
    public String getProcessingStatistics() {
        try {
            var stats = notificationQueueService.getNotificationStatistics();
            
            StringBuilder report = new StringBuilder();
            report.append("Notification Queue Statistics:\n");
            
            for (var entry : stats.entrySet()) {
                report.append(String.format("  %s: %d\n", entry.getKey(), entry.getValue()));
            }
            
            return report.toString();
            
        } catch (Exception e) {
            LOGGER.error("Error getting processing statistics: {}", e.getMessage());
            return "Error retrieving statistics: " + e.getMessage();
        }
    }

    /**
     * Process a single notification immediately (for testing or manual processing)
     */
    public boolean processNotificationImmediately(Integer notificationId) {
        try {
            var optional = notificationQueueService.getNotificationById(notificationId);
            if (optional.isPresent()) {
                processSingleNotification(optional.get());
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Error processing notification {} immediately: {}", notificationId, e.getMessage());
            return false;
        }
    }

    /**
     * Get queue health status
     */
    @Transactional(readOnly = true)
    public String getQueueHealthStatus() {
        try {
            long pendingCount = notificationQueueService.countByStatus(NotificationStatus.PENDING);
            long processingCount = notificationQueueService.countByStatus(NotificationStatus.PROCESSING);
            long failedCount = notificationQueueService.countByStatus(NotificationStatus.FAILED);
            long sentCount = notificationQueueService.countByStatus(NotificationStatus.SENT);
            
            return String.format("Queue Health - Pending: %d, Processing: %d, Failed: %d, Sent: %d", 
                               pendingCount, processingCount, failedCount, sentCount);
        } catch (Exception e) {
            LOGGER.error("Error getting queue health status: {}", e.getMessage());
            return "Error retrieving queue health: " + e.getMessage();
        }
    }
}
