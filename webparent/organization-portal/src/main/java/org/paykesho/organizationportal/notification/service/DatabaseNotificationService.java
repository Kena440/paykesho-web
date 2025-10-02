package org.paykesho.organizationportal.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Database-driven notification service for organization-portal module
 * Uses direct database access without inter-module dependencies
 */
@Slf4j
@Service
public class DatabaseNotificationService {

    @Autowired
    private org.paykesho.organizationportal.notification.repository.NotificationRequestRepository notificationRequestRepository;

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
                notificationType, recipientEmail, subject, content, "organization-portal"
            );
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created notification request {} for {} from organization-portal module", 
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
            String subject = "Password Reset Request - Paykesho Organization Portal";
            String content = String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password for your organization account. Please click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\nPaykesho Organization Support Team",
                userName, resetUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "organization-portal", 1
            );
            request.setEntityType("ORGANIZATION_PASSWORD_RESET");
            request.setMetadata(String.format("{\"userName\":\"%s\",\"resetUrl\":\"%s\"}", userName, resetUrl));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created organization password reset notification request {} for {} from organization-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating organization password reset notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create organization password reset notification request", e);
        }
    }

    /**
     * Create member invitation notification request
     */
    @Transactional
    public NotificationRequest createMemberInvitationNotificationRequest(String recipientEmail, String organizationName, 
                                                                        String inviterName, String invitationUrl, 
                                                                        String role) {
        try {
            String subject = "Invitation to Join Organization - Paykesho";
            String content = String.format(
                "Dear Colleague,\n\n" +
                "You have been invited by %s to join the organization '%s' as a %s.\n\n" +
                "Please click the link below to accept the invitation:\n\n" +
                "%s\n\n" +
                "This invitation will expire in 7 days.\n\n" +
                "Best regards,\nPaykesho Team",
                inviterName, organizationName, role, invitationUrl
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "organization-portal"
            );
            request.setEntityType("MEMBER_INVITATION");
            request.setMetadata(String.format("{\"organizationName\":\"%s\",\"inviterName\":\"%s\",\"invitationUrl\":\"%s\",\"role\":\"%s\"}", 
                             organizationName, inviterName, invitationUrl, role));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created member invitation notification request {} for {} from organization-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating member invitation notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create member invitation notification request", e);
        }
    }

    /**
     * Create organization update notification request
     */
    @Transactional
    public NotificationRequest createOrganizationUpdateNotificationRequest(String recipientEmail, String organizationName, 
                                                                        String updateType, String updateDetails) {
        try {
            String subject = "Organization Update - Paykesho";
            String content = String.format(
                "Dear Team Member,\n\n" +
                "There has been an update to your organization '%s':\n\n" +
                "Update Type: %s\n" +
                "Details: %s\n\n" +
                "Please log in to your organization portal to view the full details.\n\n" +
                "Best regards,\nPaykesho Organization Team",
                organizationName, updateType, updateDetails
            );
            
            NotificationRequest request = new NotificationRequest(
                NotificationType.email, recipientEmail, subject, content, "organization-portal"
            );
            request.setEntityType("ORGANIZATION_UPDATE");
            request.setMetadata(String.format("{\"organizationName\":\"%s\",\"updateType\":\"%s\",\"updateDetails\":\"%s\"}", 
                             organizationName, updateType, updateDetails));
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created organization update notification request {} for {} from organization-portal module", 
                    saved.getId(), recipientEmail);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating organization update notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create organization update notification request", e);
        }
    }

    /**
     * Create SMS notification request
     */
    @Transactional
    public NotificationRequest createSmsNotificationRequest(String recipientPhone, String content) {
        try {
            NotificationRequest request = new NotificationRequest(
                NotificationType.sms, recipientPhone, "SMS Notification", content, "organization-portal"
            );
            request.setRecipientPhone(recipientPhone);
            
            NotificationRequest saved = notificationRequestRepository.save(request);
            log.info("Created SMS notification request {} for {} from organization-portal module", 
                    saved.getId(), recipientPhone);
            
            return saved;
        } catch (Exception e) {
            log.error("Error creating SMS notification request: {}", e.getMessage());
            throw new RuntimeException("Failed to create SMS notification request", e);
        }
    }

    /**
     * Get notification statistics for organization-portal module
     */
    public Map<String, Object> getNotificationStatistics() {
        try {
            long total = notificationRequestRepository.countBySourceModule("organization-portal");
            long pending = notificationRequestRepository.countBySourceModuleAndStatus("organization-portal", NotificationStatus.PENDING);
            long sent = notificationRequestRepository.countBySourceModuleAndStatus("organization-portal", NotificationStatus.SENT);
            long failed = notificationRequestRepository.countBySourceModuleAndStatus("organization-portal", NotificationStatus.FAILED);
            
            return Map.of(
                "total", total,
                "pending", pending,
                "sent", sent,
                "failed", failed,
                "module", "organization-portal"
            );
        } catch (Exception e) {
            log.error("Error getting notification statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
