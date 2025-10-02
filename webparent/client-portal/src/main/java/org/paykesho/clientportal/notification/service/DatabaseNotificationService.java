package org.paykesho.clientportal.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Database-driven notification service for client-portal module
 * Uses direct database access without inter-module dependencies
 */
@Slf4j
@Service
public class DatabaseNotificationService {

    @Autowired
    private org.paykesho.clientportal.notification.repository.NotificationRequestRepository notificationRequestRepository;

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
                notificationType, recipientEmail, subject, content, "client-portal"
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from client-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create notification request", e);
        }
    }

    /**
     * Create password reset notification request
     */
    @Transactional
    public NotificationRequest createPasswordResetNotificationRequest(String recipientEmail, 
                                                                    String userName, 
                                                                    String resetUrl) {
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
                NotificationType.email, recipientEmail, subject, content, "client-portal", 1
            );
            request.setEntityType("PASSWORD_RESET");
            request.setMetadata(String.format("{\"userName\":\"%s\",\"resetUrl\":\"%s\"}", userName, resetUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created password reset notification request {} for {} from client-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating password reset notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create password reset notification request", e);
        }
    }

    /**
     * Create account verification notification request
     */
    @Transactional
    public NotificationRequest createAccountVerificationNotificationRequest(String userName, String userEmail, 
                                                                          String verificationUrl) {
        try {
            String subject = "Verify Your Paykesho Account";
            String content = String.format(
                "Dear %s,\n\n" +
                "Welcome to Paykesho! Please verify your account by clicking the link below:\n\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Best regards,\nPaykesho Team",
                userName, verificationUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, userEmail, subject, content, "client-portal"
            );
            request.setEntityType("ACCOUNT_VERIFICATION");
            request.setMetadata(String.format("{\"userName\":\"%s\",\"verificationUrl\":\"%s\"}", userName, verificationUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created account verification notification request {} for {} from client-portal module", 
                    saved.getId(), userEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating account verification notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create account verification notification request", e);
        }
    }

    /**
     * Create transaction notification request
     */
    @Transactional
    public NotificationRequest createTransactionNotificationRequest(String recipientEmail, String transactionType, 
                                                                String amount, String reference) {
        try {
            String subject = "Transaction Notification - Paykesho";
            String content = String.format(
                "Dear Customer,\n\n" +
                "Your %s transaction has been processed.\n" +
                "- Amount: %s\n" +
                "- Reference: %s\n\n" +
                "Thank you for using Paykesho!\n\n" +
                "Best regards,\nPaykesho Team",
                transactionType, amount, reference
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "client-portal"
            );
            request.setEntityType("TRANSACTION");
            request.setEntityReference(reference);
            request.setMetadata(String.format("{\"transactionType\":\"%s\",\"amount\":\"%s\",\"reference\":\"%s\"}", 
                             transactionType, amount, reference));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created transaction notification request {} for {} from client-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating transaction notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create transaction notification request", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, "client-portal"
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from client-portal module", 
                    saved.getId(), recipientPhone);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Get notification statistics for client-portal module
     */
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.countBySourceModule("client-portal");
            long pending = notificationRequestRepository.countBySourceModuleAndStatus("client-portal", NotificationStatus.PENDING);
            long sent = notificationRequestRepository.countBySourceModuleAndStatus("client-portal", NotificationStatus.SENT);
            long failed = notificationRequestRepository.countBySourceModuleAndStatus("client-portal", NotificationStatus.FAILED);
            
            return Map.of(
                "total", total,
                "pending", pending,
                "sent", sent,
                "failed", failed,
                "module", "client-portal"
            );
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
