package org.paykesho.backoffice.notification.general;

import lombok.extern.slf4j.Slf4j;
import org.commons.enums.NotificationType;
import org.commons.utils.NotificationTypeUtils;
import org.paykesho.backoffice.notification.service.DatabaseNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationClient {
    
    @Autowired
    private DatabaseNotificationService databaseNotificationService;

    public boolean sendNotification(String recipient, String message, String notificationType) {
        try {
            log.info("Creating notification request via database for {} from backoffice module", recipient);
            
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

    public boolean sendMerchantAdminWelcomeEmail(String adminName, String adminEmail, String merchantName, String portalUrl, String temporaryPassword) {
        try {
            databaseNotificationService.createMerchantAdminWelcomeNotificationRequest(
                adminName, adminEmail, merchantName, portalUrl, temporaryPassword
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to create merchant admin welcome notification request: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendOrganizationAdminWelcomeEmail(String adminName, String adminEmail, String organizationName, String portalUrl, String temporaryPassword) {
        try {
            databaseNotificationService.createOrganizationAdminWelcomeNotificationRequest(
                adminName, adminEmail, organizationName, portalUrl, temporaryPassword
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to create organization admin welcome notification request: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String userName, String userEmail, String resetUrl) {
        try {
            databaseNotificationService.createPasswordResetNotificationRequest(
                userEmail, userName, resetUrl
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to create password reset notification request: {}", e.getMessage());
            return false;
        }
    }
}