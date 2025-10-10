package org.paykesho.clientportal.notification.general;

import lombok.extern.slf4j.Slf4j;
import org.commons.enums.NotificationType;
import org.commons.utils.NotificationTypeUtils;
import org.paykesho.clientportal.notification.service.DatabaseNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationClient {
    
    @Autowired
    private DatabaseNotificationService databaseNotificationService;

    public boolean sendNotification(String recipient, String message, String notificationType) {
        try {
            log.info("Creating notification request via database for {} from client-portal module", recipient);
            
            // Resilient enum parsing with email fallback
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            
            databaseNotificationService.createNotificationRequest(
                type, recipient, "Notification", message
            );
            
            log.info("Created notification request for {} via database", recipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to create notification request: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String userName, String userEmail, String resetUrl) {
        try {
            log.info("Creating password reset notification request for {} from client-portal module", userEmail);
            databaseNotificationService.createPasswordResetNotificationRequest(
                userEmail, userName, resetUrl
            );
            log.info("Created password reset notification request for {} via database", userEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to create password reset notification request: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendVoucherNotificationEmail(String clientName, String clientEmail, String voucherReference, 
                                               String actionType, String message, java.math.BigDecimal amount) {
        try {
            log.info("Creating voucher notification request for {} from client-portal module", clientEmail);
            databaseNotificationService.createVoucherNotificationRequest(
                clientEmail, clientName, voucherReference, actionType, message, amount
            );
            log.info("Created voucher notification request for {} via database", clientEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to create voucher notification request: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendTransactionNotificationEmail(String clientName, String clientEmail, String transactionReference, 
                                                   String merchantName, java.math.BigDecimal amount) {
        try {
            log.info("Creating transaction notification request for {} from client-portal module", clientEmail);
            databaseNotificationService.createTransactionNotificationRequest(
                clientEmail, "PAYMENT", amount.toString(), transactionReference
            );
            log.info("Created transaction notification request for {} via database", clientEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to create transaction notification request: {}", e.getMessage());
            return false;
        }
    }
}