package org.paykesho.backoffice.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Database-driven notification service for backoffice module
 * Uses direct database access without inter-module dependencies
 */
@Slf4j
@Service
public class DatabaseNotificationService {

    @Autowired
    private org.paykesho.backoffice.notification.repository.NotificationRequestRepository notificationRequestRepository;

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
                notificationType, recipientEmail, subject, content, "backoffice"
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from backoffice module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create notification request", e);
        }
    }

    /**
     * Create a high priority notification request
     */
    @Transactional
    public NotificationRequest createHighPriorityNotificationRequest(NotificationType notificationType, 
                                                                   String recipientEmail, 
                                                                   String subject, 
                                                                   String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                notificationType, recipientEmail, subject, content, "backoffice", 1
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created high priority notification request {} for {} from backoffice module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating high priority notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create high priority notification request", e);
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
                NotificationType.email, recipientEmail, subject, content, "backoffice", 1
            );
            request.setEntityType("PASSWORD_RESET");
            request.setMetadata(String.format("{\"userName\":\"%s\",\"resetUrl\":\"%s\"}", userName, resetUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created password reset notification request {} for {} from backoffice module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating password reset notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create password reset notification request", e);
        }
    }

    /**
     * Create merchant admin welcome notification request
     */
    @Transactional
    public NotificationRequest createMerchantAdminWelcomeNotificationRequest(String adminName, String adminEmail, 
                                                                          String merchantName, String portalUrl, 
                                                                          String temporaryPassword) {
        try {
            String subject = "Welcome to Paykesho Merchant Portal";
            String content = String.format(
                "Dear %s,\n\n" +
                "Welcome to the Paykesho Merchant Portal for %s!\n\n" +
                "Your account has been created with the following details:\n" +
                "- Portal URL: %s\n" +
                "- Temporary Password: %s\n\n" +
                "Please log in and change your password immediately.\n\n" +
                "Best regards,\nPaykesho Team",
                adminName, merchantName, portalUrl, temporaryPassword
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, adminEmail, subject, content, "backoffice"
            );
            request.setEntityType("MERCHANT_ADMIN_WELCOME");
            request.setMetadata(String.format("{\"adminName\":\"%s\",\"merchantName\":\"%s\",\"portalUrl\":\"%s\",\"temporaryPassword\":\"%s\"}", 
                             adminName, merchantName, portalUrl, temporaryPassword));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created merchant admin welcome notification request {} for {} from backoffice module", 
                    saved.getId(), adminEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating merchant admin welcome notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create merchant admin welcome notification request", e);
        }
    }

    /**
     * Create organization admin welcome notification request
     */
    @Transactional
    public NotificationRequest createOrganizationAdminWelcomeNotificationRequest(String adminName, String adminEmail, 
                                                                              String organizationName, String portalUrl, 
                                                                              String temporaryPassword) {
        try {
            String subject = "Welcome to Paykesho Organization Portal";
            String content = String.format(
                "Dear %s,\n\n" +
                "Welcome to the Paykesho Organization Portal for %s!\n\n" +
                "Your account has been created with the following details:\n" +
                "- Portal URL: %s\n" +
                "- Temporary Password: %s\n\n" +
                "Please log in and change your password immediately.\n\n" +
                "Best regards,\nPaykesho Team",
                adminName, organizationName, portalUrl, temporaryPassword
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, adminEmail, subject, content, "backoffice"
            );
            request.setEntityType("ORGANIZATION_ADMIN_WELCOME");
            request.setMetadata(String.format("{\"adminName\":\"%s\",\"organizationName\":\"%s\",\"portalUrl\":\"%s\",\"temporaryPassword\":\"%s\"}", 
                             adminName, organizationName, portalUrl, temporaryPassword));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created organization admin welcome notification request {} for {} from backoffice module", 
                    saved.getId(), adminEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating organization admin welcome notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create organization admin welcome notification request", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, "backoffice"
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from backoffice module", 
                    saved.getId(), recipientPhone);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Get notification statistics for backoffice module
     */
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.countBySourceModule("backoffice");
            long pending = notificationRequestRepository.countBySourceModuleAndStatus("backoffice", NotificationStatus.PENDING);
            long sent = notificationRequestRepository.countBySourceModuleAndStatus("backoffice", NotificationStatus.SENT);
            long failed = notificationRequestRepository.countBySourceModuleAndStatus("backoffice", NotificationStatus.FAILED);
            
            return Map.of(
                "total", total,
                "pending", pending,
                "sent", sent,
                "failed", failed,
                "module", "backoffice"
            );
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Create admin password reset notification request
     */
    @Transactional
    public NotificationRequest createAdminPasswordResetNotificationRequest(String adminEmail, String adminName, 
                                                                         String portalUrl, String temporaryPassword, 
                                                                         String entityName) {
        try {
            String subject = "Password Reset - Paykesho Admin";
            String content = String.format(
                "Dear %s,\n\n" +
                "Your password has been reset for your %s admin account.\n\n" +
                "New Login Credentials:\n" +
                "- Portal URL: %s\n" +
                "- Email: %s\n" +
                "- New Temporary Password: %s\n\n" +
                "Important Security Notice:\n" +
                "- You must change this password immediately after your first login\n" +
                "- This temporary password will expire in 24 hours\n" +
                "- Use a strong, unique password with at least 8 characters\n" +
                "- Include uppercase, lowercase, numbers, and special characters\n" +
                "- Never share your credentials with others\n" +
                "- If you did not request this password reset, contact support immediately\n\n" +
                "Best regards,\nPaykesho Team",
                adminName, entityName, portalUrl, adminEmail, temporaryPassword
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, adminEmail, subject, content, "backoffice"
            );
            request.setEntityType("ADMIN_PASSWORD_RESET");
            request.setMetadata(String.format("{\"adminName\":\"%s\",\"portalUrl\":\"%s\",\"entityName\":\"%s\"}", 
                             adminName, portalUrl, entityName));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created admin password reset notification request {} for {} from backoffice module", 
                    saved.getId(), adminEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating admin password reset notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create admin password reset notification request", e);
        }
    }
}
