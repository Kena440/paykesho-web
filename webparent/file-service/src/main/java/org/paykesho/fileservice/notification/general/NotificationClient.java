package org.paykesho.fileservice.notification.general;

import lombok.extern.slf4j.Slf4j;
import org.commons.enums.NotificationType;
import org.commons.utils.NotificationTypeUtils;
import org.paykesho.fileservice.notification.service.DatabaseNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationClient {
    
    @Autowired
    private DatabaseNotificationService databaseNotificationService;

    public boolean sendNotification(String recipient, String message, String notificationType) {
        try {
            log.info("Creating notification request via database for {} from file-service module", recipient);
            
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
}