package org.paykesho.paymentservice.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Database-driven notification service for payment-service module
 * Uses direct database access without inter-module dependencies
 */
@Slf4j
@Service
public class DatabaseNotificationService {

    @Autowired
    private org.paykesho.paymentservice.notification.repository.NotificationRequestRepository notificationRequestRepository;

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
                notificationType, recipientEmail, subject, content, "payment-service"
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from payment-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create notification request", e);
        }
    }

    /**
     * Create payment success notification request
     */
    @Transactional
    public NotificationRequest createPaymentSuccessNotificationRequest(String recipientEmail, String customerName, 
                                                                    String paymentAmount, String paymentReference, 
                                                                    String merchantName) {
        try {
            String subject = "Payment Successful - Paykesho";
            String content = String.format(
                "Dear %s,\n\n" +
                "Your payment has been processed successfully!\n\n" +
                "Payment Details:\n" +
                "- Amount: %s\n" +
                "- Reference: %s\n" +
                "- Merchant: %s\n\n" +
                "Thank you for using Paykesho!\n\n" +
                "Best regards,\nPaykesho Payment Team",
                customerName, paymentAmount, paymentReference, merchantName
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "payment-service", 1
            );
            request.setEntityType("PAYMENT_SUCCESS");
            request.setEntityReference(paymentReference);
            request.setMetadata(String.format("{\"customerName\":\"%s\",\"paymentAmount\":\"%s\",\"paymentReference\":\"%s\",\"merchantName\":\"%s\"}", 
                             customerName, paymentAmount, paymentReference, merchantName));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created payment success notification request {} for {} from payment-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating payment success notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment success notification request", e);
        }
    }

    /**
     * Create payment failure notification request
     */
    @Transactional
    public NotificationRequest createPaymentFailureNotificationRequest(String recipientEmail, String customerName, 
                                                                    String paymentAmount, String paymentReference, 
                                                                    String failureReason) {
        try {
            String subject = "Payment Failed - Paykesho";
            String content = String.format(
                "Dear %s,\n\n" +
                "Unfortunately, your payment could not be processed.\n\n" +
                "Payment Details:\n" +
                "- Amount: %s\n" +
                "- Reference: %s\n" +
                "- Reason: %s\n\n" +
                "Please try again or contact support if the issue persists.\n\n" +
                "Best regards,\nPaykesho Payment Team",
                customerName, paymentAmount, paymentReference, failureReason
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "payment-service", 1
            );
            request.setEntityType("PAYMENT_FAILURE");
            request.setEntityReference(paymentReference);
            request.setMetadata(String.format("{\"customerName\":\"%s\",\"paymentAmount\":\"%s\",\"paymentReference\":\"%s\",\"failureReason\":\"%s\"}", 
                             customerName, paymentAmount, paymentReference, failureReason));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created payment failure notification request {} for {} from payment-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating payment failure notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment failure notification request", e);
        }
    }

    /**
     * Create refund notification request
     */
    @Transactional
    public NotificationRequest createRefundNotificationRequest(String recipientEmail, String customerName, 
                                                            String refundAmount, String originalPaymentReference, 
                                                            String refundReference) {
        try {
            String subject = "Refund Processed - Paykesho";
            String content = String.format(
                "Dear %s,\n\n" +
                "Your refund has been processed successfully!\n\n" +
                "Refund Details:\n" +
                "- Amount: %s\n" +
                "- Original Payment: %s\n" +
                "- Refund Reference: %s\n\n" +
                "The refund will appear in your account within 3-5 business days.\n\n" +
                "Best regards,\nPaykesho Payment Team",
                customerName, refundAmount, originalPaymentReference, refundReference
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "payment-service"
            );
            request.setEntityType("REFUND_PROCESSED");
            request.setEntityReference(refundReference);
            request.setMetadata(String.format("{\"customerName\":\"%s\",\"refundAmount\":\"%s\",\"originalPaymentReference\":\"%s\",\"refundReference\":\"%s\"}", 
                             customerName, refundAmount, originalPaymentReference, refundReference));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created refund notification request {} for {} from payment-service module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating refund notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create refund notification request", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, "payment-service"
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from payment-service module", 
                    saved.getId(), recipientPhone);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Get notification statistics for payment-service module
     */
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.countBySourceModule("payment-service");
            long pending = notificationRequestRepository.countBySourceModuleAndStatus("payment-service", NotificationStatus.PENDING);
            long sent = notificationRequestRepository.countBySourceModuleAndStatus("payment-service", NotificationStatus.SENT);
            long failed = notificationRequestRepository.countBySourceModuleAndStatus("payment-service", NotificationStatus.FAILED);
            
            return Map.of(
                "total", total,
                "pending", pending,
                "sent", sent,
                "failed", failed,
                "module", "payment-service"
            );
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
