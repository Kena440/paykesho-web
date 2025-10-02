package org.paykesho.fileservice.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Database-driven notification service for file-service module
 * Uses direct database access without inter-module dependencies
 */
@Slf4j
@Service
public class DatabaseNotificationService {

    @Autowired
    private org.paykesho.fileservice.notification.repository.NotificationRequestRepository notificationRequestRepository;

    /**
     * Create a basic notification request
     */
    @Transactional
    public NotificationRequest createNotificationRequest(NotificationType notificationType, 
                                                       String recipientEmail, 
                                                       String subject, 
                                                       String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                notificationType, recipientEmail, subject, content, "file-service"
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from file-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create notification request", e);
        }
    }

    /**
     * Create file upload notification request
     */
    @Transactional
    public NotificationRequest createFileUploadNotificationRequest(String recipientEmail, 
                                                                 String fileName, 
                                                                 String fileUrl) {
        try {
            String subject = "File Upload Complete - Paykesho";
            String content = String.format(
                "Hello,\n\n" +
                "Your file '%s' has been successfully uploaded.\n\n" +
                "You can access it at: %s\n\n" +
                "Best regards,\nPaykesho File Service",
                fileName, fileUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "file-service"
            );
            request.setEntityType("FILE_UPLOAD");
            request.setMetadata(String.format("{\"fileName\":\"%s\",\"fileUrl\":\"%s\"}", fileName, fileUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created file upload notification request {} for {} from file-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating file upload notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create file upload notification request", e);
        }
    }

    /**
     * Create file processing notification request
     */
    @Transactional
    public NotificationRequest createFileProcessingNotificationRequest(String recipientEmail, 
                                                                     String fileName, 
                                                                     String processingStatus) {
        try {
            String subject = "File Processing Update - Paykesho";
            String content = String.format(
                "Hello,\n\n" +
                "Your file '%s' processing status: %s\n\n" +
                "You will be notified when processing is complete.\n\n" +
                "Best regards,\nPaykesho File Service",
                fileName, processingStatus
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "file-service"
            );
            request.setEntityType("FILE_PROCESSING");
            request.setMetadata(String.format("{\"fileName\":\"%s\",\"processingStatus\":\"%s\"}", fileName, processingStatus));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created file processing notification request {} for {} from file-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating file processing notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create file processing notification request", e);
        }
    }

    /**
     * Create file download notification request
     */
    @Transactional
    public NotificationRequest createFileDownloadNotificationRequest(String recipientEmail, 
                                                                    String fileName, 
                                                                    String downloadUrl) {
        try {
            String subject = "File Download Ready - Paykesho";
            String content = String.format(
                "Hello,\n\n" +
                "Your requested file '%s' is ready for download.\n\n" +
                "Download link: %s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Best regards,\nPaykesho File Service",
                fileName, downloadUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "file-service"
            );
            request.setEntityType("FILE_DOWNLOAD");
            request.setMetadata(String.format("{\"fileName\":\"%s\",\"downloadUrl\":\"%s\"}", fileName, downloadUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created file download notification request {} for {} from file-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating file download notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create file download notification request", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, "file-service"
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from file-service module", 
                    saved.getId(), recipientPhone);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Get notification statistics for file-service module
     */
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.countBySourceModule("file-service");
            long pending = notificationRequestRepository.countBySourceModuleAndStatus("file-service", NotificationStatus.PENDING);
            long sent = notificationRequestRepository.countBySourceModuleAndStatus("file-service", NotificationStatus.SENT);
            long failed = notificationRequestRepository.countBySourceModuleAndStatus("file-service", NotificationStatus.FAILED);
            
            return Map.of(
                "total", total,
                "pending", pending,
                "sent", sent,
                "failed", failed,
                "module", "file-service"
            );
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
