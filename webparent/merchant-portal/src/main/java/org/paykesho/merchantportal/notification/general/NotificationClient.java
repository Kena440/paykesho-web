package org.paykesho.merchantportal.notification.general;

import lombok.extern.slf4j.Slf4j;
import org.commons.enums.NotificationType;
import org.commons.utils.NotificationTypeUtils;
import org.paykesho.merchantportal.notification.service.DatabaseNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationClient {
    
    @Autowired
    private DatabaseNotificationService databaseNotificationService;

    public boolean sendNotification(String recipient, String message, String notificationType) {
        try {
            log.info("Creating notification request via database for {} from merchant-portal module", recipient);
            
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
            log.info("Creating password reset notification request for {} from merchant-portal module", userEmail);
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
}