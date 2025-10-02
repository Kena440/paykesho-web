package org.paykesho.merchantportal.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Database-driven notification service for merchant-portal module
 * Uses direct database access without inter-module dependencies
 */
@Slf4j
@Service
public class DatabaseNotificationService {

    @Autowired
    private org.paykesho.merchantportal.notification.repository.NotificationRequestRepository notificationRequestRepository;

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
                notificationType, recipientEmail, subject, content, "merchant-portal"
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from merchant-portal module", 
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
            String subject = "Password Reset Request - Paykesho Merchant Portal";
            String content = String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password for your merchant account. Please click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\nPaykesho Merchant Support Team",
                userName, resetUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "merchant-portal", 1
            );
            request.setEntityType("MERCHANT_PASSWORD_RESET");
            request.setMetadata(String.format("{\"userName\":\"%s\",\"resetUrl\":\"%s\"}", userName, resetUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created merchant password reset notification request {} for {} from merchant-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating merchant password reset notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create merchant password reset notification request", e);
        }
    }

    /**
     * Create payment notification request
     */
    @Transactional
    public NotificationRequest createPaymentNotificationRequest(String recipientEmail, String merchantName, 
                                                            String paymentAmount, String paymentReference, 
                                                            String customerName) {
        try {
            String subject = "New Payment Received - Paykesho";
            String content = String.format(
                "Dear %s,\n\n" +
                "You have received a new payment!\n\n" +
                "Payment Details:\n" +
                "- Amount: %s\n" +
                "- Reference: %s\n" +
                "- Customer: %s\n\n" +
                "This payment has been processed and is now available in your merchant account.\n\n" +
                "Best regards,\nPaykesho Team",
                merchantName, paymentAmount, paymentReference, customerName
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "merchant-portal"
            );
            request.setEntityType("PAYMENT_RECEIVED");
            request.setEntityReference(paymentReference);
            request.setMetadata(String.format("{\"merchantName\":\"%s\",\"paymentAmount\":\"%s\",\"paymentReference\":\"%s\",\"customerName\":\"%s\"}", 
                             merchantName, paymentAmount, paymentReference, customerName));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created payment notification request {} for {} from merchant-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating payment notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment notification request", e);
        }
    }

    /**
     * Create settlement notification request
     */
    @Transactional
    public NotificationRequest createSettlementNotificationRequest(String recipientEmail, String merchantName, 
                                                                String settlementAmount, String settlementDate, 
                                                                String bankAccount) {
        try {
            String subject = "Settlement Processed - Paykesho";
            String content = String.format(
                "Dear %s,\n\n" +
                "Your settlement has been processed successfully!\n\n" +
                "Settlement Details:\n" +
                "- Amount: %s\n" +
                "- Date: %s\n" +
                "- Bank Account: %s\n\n" +
                "The funds should appear in your bank account within 1-2 business days.\n\n" +
                "Best regards,\nPaykesho Settlement Team",
                merchantName, settlementAmount, settlementDate, bankAccount
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "merchant-portal"
            );
            request.setEntityType("SETTLEMENT_PROCESSED");
            request.setMetadata(String.format("{\"merchantName\":\"%s\",\"settlementAmount\":\"%s\",\"settlementDate\":\"%s\",\"bankAccount\":\"%s\"}", 
                             merchantName, settlementAmount, settlementDate, bankAccount));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created settlement notification request {} for {} from merchant-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating settlement notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create settlement notification request", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, "merchant-portal"
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from merchant-portal module", 
                    saved.getId(), recipientPhone);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Get notification statistics for merchant-portal module
     */
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.countBySourceModule("merchant-portal");
            long pending = notificationRequestRepository.countBySourceModuleAndStatus("merchant-portal", NotificationStatus.PENDING);
            long sent = notificationRequestRepository.countBySourceModuleAndStatus("merchant-portal", NotificationStatus.SENT);
            long failed = notificationRequestRepository.countBySourceModuleAndStatus("merchant-portal", NotificationStatus.FAILED);
            
            return Map.of(
                "total", total,
                "pending", pending,
                "sent", sent,
                "failed", failed,
                "module", "merchant-portal"
            );
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
