package org.paykesho.notificationservice.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Notification;
import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.commons.utils.NotificationTypeUtils;
import org.paykesho.notificationservice.notifications.repository.NotificationsRepository;
import org.paykesho.notificationservice.repository.NotificationQueueRepository;
import org.paykesho.notificationservice.utils.EmailTemplateService;
import org.paykesho.notificationservice.utils.general.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_MESSAGE_LENGTH = 200;

    private final RestTemplate restTemplate = new RestTemplate();
    private final NotificationsRepository notificationRepository;
    private final ApplicationProperties applicationProperties;
    private final EmailTemplateService emailTemplateService;
    
    @Autowired
    private NotificationQueueRepository notificationQueueRepository;

    public static String trimString(String input) {
        if (input != null && input.length() > MAX_MESSAGE_LENGTH) {
            return input.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return input;
    }

    /**
     * Sanitize input to prevent XSS and other security issues
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    @Transactional
    public boolean sendNotification(String recipient, String message, String notificationType) {
        // Enhanced input validation
        if (notificationType == null || recipient == null || message == null) {
            logger.error("Invalid input: recipient={}, message={}, notificationType={}",
                    recipient != null ? recipient : "null",
                    message != null ? "provided" : "null",
                    notificationType);
            return false;
        }

        // Note: NotificationTypeUtils.parseNotificationType() handles validation and defaults to email

        // Validate recipient format
        if (recipient.trim().isEmpty()) {
            logger.error("Recipient cannot be empty");
            return false;
        }

        if (message.trim().isEmpty()) {
            logger.error("Message cannot be empty");
            return false;
        }
        Notification saved = null;
        // Save notification to DB before sending
        try {
            Notification notification = new Notification();
            notification.setReference(UUID.randomUUID().toString());
            notification.setRecipient(recipient);
            notification.setMessage(trimString(message));
            notification.setNotificationType(NotificationTypeUtils.parseNotificationType(notificationType));
            notification.setStatus("pending");
            saved = notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error saving notification to database: {}", e.getMessage());
        }
        boolean result;
        if (notificationType.equalsIgnoreCase("sms")) {
            result = sendSms(recipient, message);
        } else if (notificationType.equalsIgnoreCase("email")) {
            result = sendEmail(recipient, "Notification", message);
        } else {
            logger.error("Unsupported notification type: {}", notificationType);
            return false;
        }

        try {
            if (saved != null && saved.getId() != null) {
                Notification notification = notificationRepository.findById(saved.getId()).orElse(null);
                if (notification != null) {
                    notification.setStatus(result ? "sent" : "failed");
                    notificationRepository.save(notification);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating notification status: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Send welcome email to new admin users using template
     */
    public boolean sendWelcomeAdminEmail(String adminName, String adminEmail, String portalUrl, String temporaryPassword) {
        try {
            String emailContent = emailTemplateService.generateWelcomeAdminEmail(adminName, adminEmail, portalUrl, temporaryPassword);
            return sendEmail(adminEmail, "Welcome to Paykesho - Admin Account Created", emailContent);
        } catch (Exception e) {
            logger.error("Error sending welcome admin email: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send welcome email to merchant admin using template
     */
    public boolean sendMerchantAdminWelcomeEmail(String adminName, String adminEmail, String merchantName, String portalUrl, String temporaryPassword) {
        try {
            logger.info("Generating merchant admin welcome email for: {} ({})", adminName, adminEmail);
            String emailContent = emailTemplateService.generateMerchantAdminWelcomeEmail(adminName, adminEmail, merchantName, portalUrl, temporaryPassword);
            logger.info("Generated email content length: {} characters", emailContent != null ? emailContent.length() : 0);
            logger.info("Email content preview: {}", emailContent != null ? emailContent.substring(0, Math.min(200, emailContent.length())) + "..." : "null");

            boolean result = sendEmail(adminEmail, "Welcome to Paykesho Merchant Portal", emailContent);
            logger.info("Merchant admin welcome email sent result: {} for {}", result, adminEmail);
            return result;
        } catch (Exception e) {
            logger.error("Error sending merchant admin welcome email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send welcome email to organization admin using template
     */
    public boolean sendOrganizationAdminWelcomeEmail(String adminName, String adminEmail, String organizationName, String portalUrl, String temporaryPassword) {
        try {
            logger.info("Generating organization admin welcome email for: {} ({})", adminName, adminEmail);
            String emailContent = emailTemplateService.generateOrganizationAdminWelcomeEmail(adminName, adminEmail, organizationName, portalUrl, temporaryPassword);
            logger.info("Generated email content length: {} characters", emailContent != null ? emailContent.length() : 0);
            logger.info("Email content preview: {}", emailContent != null ? emailContent.substring(0, Math.min(200, emailContent.length())) + "..." : "null");

            boolean result = sendEmail(adminEmail, "Welcome to Paykesho Organization Portal", emailContent);
            logger.info("Organization admin welcome email sent result: {} for {}", result, adminEmail);
            return result;
        } catch (Exception e) {
            logger.error("Error sending organization admin welcome email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send password reset email using template
     */
    public boolean sendPasswordResetEmail(String userName, String userEmail, String resetUrl) {
        try {
            String emailContent = emailTemplateService.generatePasswordResetEmail(userName, userEmail, resetUrl);
            return sendEmail(userEmail, "Password Reset Request - Paykesho", emailContent);
        } catch (Exception e) {
            logger.error("Error sending password reset email: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send account locked notification email using template
     */
    public boolean sendAccountLockedEmail(String userName, String userEmail, LocalDateTime lockTime, LocalDateTime unlockTime) {
        try {
            String emailContent = emailTemplateService.generateAccountLockedEmail(userName, userEmail, lockTime, unlockTime);
            return sendEmail(userEmail, "Account Security Alert - Paykesho", emailContent);
        } catch (Exception e) {
            logger.error("Error sending account locked email: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send custom templated email
     */
    public boolean sendCustomTemplatedEmail(String recipient, String subject, String greeting, String content,
                                            String highlightTitle, String highlightContent,
                                            String warningTitle, String warningContent,
                                            String actionUrl, String actionText) {
        try {
            String emailContent = emailTemplateService.generateCustomEmail(subject, greeting, content, recipient,
                    highlightTitle, highlightContent, warningTitle, warningContent, actionUrl, actionText);
            return sendEmail(recipient, subject, emailContent);
        } catch (Exception e) {
            logger.error("Error sending custom templated email: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendSms(String phoneNumber, String message) {
        try {
            String trimmedMessage = trimString(message);
            logger.info("Sending SMS to: {} with message: {}", phoneNumber, trimmedMessage);

            // Remove URL encoding as it's causing malformed SMS messages
            // String encodedMessage = URLEncoder.encode(trimmedMessage, StandardCharsets.UTF_8);
            String apiUrl = String.format(
                    "https://www.smszambia.com/smsservice/httpapi?username=%s&password=%s&msg=%s&sender_id=%s&phone=%s",
                    applicationProperties.getUsername(), applicationProperties.getPassword(), trimmedMessage, applicationProperties.getSenderId(), phoneNumber
            );
            logger.info("SMS API URL (without credentials): https://www.smszambia.com/smsservice/httpapi?username=***&password=***&msg={}&sender_id={}&phone={}",
                    trimmedMessage, applicationProperties.getSenderId(), phoneNumber);

            String response = restTemplate.getForObject(apiUrl, String.class);
            boolean ok = response != null && response.toLowerCase().contains("ok");
            logger.info("SMS API response: {}", response);
            if (!ok) logger.error("SMS API response: {}", response);
            return ok;
        } catch (Exception e) {
            logger.error("Error sending SMS: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendEmail(String recipient, String subject, String message) {
        try {
            // Input sanitization
            if (recipient == null || recipient.trim().isEmpty()) {
                logger.error("Recipient email cannot be null or empty");
                return false;
            }
            
            // Basic email validation
            if (!recipient.contains("@") || !recipient.contains(".")) {
                logger.error("Invalid email format: {}", recipient);
                return false;
            }
            
            // Sanitize subject and message
            String sanitizedSubject = sanitizeInput(subject);
            String sanitizedMessage = sanitizeInput(message);
            
            logger.info("Sending email to: {} with subject: {}", recipient, sanitizedSubject);
            logger.info("Email message length: {} characters", sanitizedMessage != null ? sanitizedMessage.length() : 0);

            // Use sanitized content
            String emailContent = sanitizedMessage;
            logger.info("Email content length: {} characters", emailContent != null ? emailContent.length() : 0);

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", applicationProperties.getSmtpServer());
            props.put("mail.smtp.port", String.valueOf(applicationProperties.getSmtpPort()));
            props.put("mail.smtp.ssl.trust", applicationProperties.getSmtpServer());
            
            // Add timeout configurations
            props.put("mail.smtp.connectiontimeout", "10000"); // 10 seconds
            props.put("mail.smtp.timeout", "10000"); // 10 seconds
            props.put("mail.smtp.writetimeout", "10000"); // 10 seconds

            logger.info("SMTP Configuration - Host: {}, Port: {}, User: {}",
                    applicationProperties.getSmtpServer(),
                    applicationProperties.getSmtpPort(),
                    applicationProperties.getSmtpUser());

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(
                            applicationProperties.getSmtpUser(),
                            applicationProperties.getSmtpPassword()
                    );
                }
            });

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(
                    applicationProperties.getFromEmail(),
                    applicationProperties.getFromName()
            ));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            mimeMessage.setSubject(sanitizedSubject);
            mimeMessage.setContent(emailContent, "text/html; charset=utf-8");

            logger.info("About to send email via SMTP...");
            Transport.send(mimeMessage);
            logger.info("Email sent successfully to: {}", recipient);
            return true;
        } catch (Exception e) {
            logger.error("Email could not be sent to {}. Error: {}", recipient, e.getMessage(), e);
            return false;
        }
    }

    // ==================== QUEUE FUNCTIONALITY ====================

    /**
     * Send notification using queue system (recommended approach)
     */
    public NotificationQueue sendNotificationQueued(String recipient, String message, String notificationType) {
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            return addNotificationToQueue(type, recipient, "Notification", message);
        } catch (Exception e) {
            logger.error("Error creating notification queue: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Send notification using queue system with subject
     */
    public NotificationQueue sendNotificationQueued(String recipient, String subject, String message, String notificationType) {
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            return addNotificationToQueue(type, recipient, subject, message);
        } catch (Exception e) {
            logger.error("Error creating notification queue: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Send high priority notification using queue system
     */
    public NotificationQueue sendHighPriorityNotification(String recipient, String subject, String message, String notificationType) {
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            NotificationQueue notification = new NotificationQueue(type, recipient, subject, message);
            notification.setPriority(1); // Highest priority
            notification = notificationQueueRepository.save(notification);
            createLegacyNotification(notification);
            return notification;
        } catch (Exception e) {
            logger.error("Error creating high priority notification: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Send scheduled notification using queue system
     */
    public NotificationQueue sendScheduledNotification(String recipient, String subject, String message, 
                                                      String notificationType, Date scheduledFor) {
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            NotificationQueue notification = new NotificationQueue(type, recipient, subject, message);
            notification.setScheduledFor(scheduledFor);
            notification = notificationQueueRepository.save(notification);
            createLegacyNotification(notification);
            return notification;
        } catch (Exception e) {
            logger.error("Error creating scheduled notification: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Send notification with metadata using queue system
     */
    public NotificationQueue sendNotificationWithMetadata(String recipient, String subject, String message, 
                                                         String notificationType, String metadata, 
                                                         String entityReference, String entityType) {
        try {
            NotificationType type = NotificationTypeUtils.parseNotificationType(notificationType);
            NotificationQueue notification = new NotificationQueue(type, recipient, subject, message);
            notification.setMetadata(metadata);
            notification.setEntityReference(entityReference);
            notification.setEntityType(entityType);
            notification = notificationQueueRepository.save(notification);
            createLegacyNotification(notification);
            return notification;
        } catch (Exception e) {
            logger.error("Error creating notification with metadata: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Add notification to queue (internal method)
     */
    private NotificationQueue addNotificationToQueue(NotificationType type, String recipient, String subject, String message) {
        NotificationQueue notification = new NotificationQueue(type, recipient, subject, message);
        notification = notificationQueueRepository.save(notification);
        createLegacyNotification(notification);
        return notification;
    }

    /**
     * Create legacy notification for backward compatibility
     */
    private void createLegacyNotification(NotificationQueue queueNotification) {
        try {
            Notification legacyNotification = new Notification();
            legacyNotification.setReference(UUID.randomUUID().toString());
            legacyNotification.setRecipient(queueNotification.getRecipientEmail());
            legacyNotification.setMessage(queueNotification.getContent());
            legacyNotification.setNotificationType(queueNotification.getNotificationType());
            legacyNotification.setStatus("pending");
            legacyNotification.setCreatedAt(new Date());
            notificationRepository.save(legacyNotification);
        } catch (Exception e) {
            logger.error("Error creating legacy notification: {}", e.getMessage());
        }
    }

    /**
     * Update legacy notification status
     */
    private void updateLegacyNotificationStatus(NotificationQueue queueNotification) {
        try {
            // Find legacy notification by recipient and message
            List<Notification> legacyNotifications = notificationRepository.findAll();
            for (Notification legacy : legacyNotifications) {
                if (legacy.getRecipient() != null && legacy.getRecipient().equals(queueNotification.getRecipientEmail()) &&
                    legacy.getMessage() != null && legacy.getMessage().equals(queueNotification.getContent())) {
                    
                    // Map queue status to legacy status
                    String legacyStatus = mapQueueStatusToLegacyStatus(queueNotification.getStatus());
                    legacy.setStatus(legacyStatus);
                    notificationRepository.save(legacy);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error updating legacy notification status: {}", e.getMessage());
        }
    }

    /**
     * Map queue status to legacy status
     */
    private String mapQueueStatusToLegacyStatus(NotificationStatus queueStatus) {
        switch (queueStatus) {
            case PENDING:
                return "pending";
            case PROCESSING:
                return "processing";
            case SENT:
                return "sent";
            case FAILED:
            case FAILED_PERMANENTLY:
                return "failed";
            case CANCELLED:
                return "cancelled";
            default:
                return "pending";
        }
    }

    // ==================== QUEUE MANAGEMENT ====================

    /**
     * Get pending notifications ready to be processed
     */
    @Transactional
    public List<NotificationQueue> getPendingNotifications() {
        return notificationQueueRepository.findPendingNotifications(NotificationStatus.PENDING, new Date());
    }

    /**
     * Get failed notifications that can be retried
     */
    @Transactional
    public List<NotificationQueue> getRetryableNotifications() {
        return notificationQueueRepository.findRetryableNotifications(NotificationStatus.FAILED, new Date());
    }

    /**
     * Get notifications by status
     */
    @Transactional
    public List<NotificationQueue> getNotificationsByStatus(NotificationStatus status) {
        return notificationQueueRepository.findByStatus(status);
    }

    /**
     * Get notifications by recipient
     */
    @Transactional
    public List<NotificationQueue> getNotificationsByRecipient(String recipientEmail) {
        return notificationQueueRepository.findByRecipientEmail(recipientEmail);
    }

    /**
     * Get notification by ID
     */
    @Transactional
    public Optional<NotificationQueue> getNotificationById(Integer id) {
        return notificationQueueRepository.findById(id);
    }

    /**
     * Update notification status
     */
    public NotificationQueue updateNotificationStatus(Integer id, NotificationStatus status) {
        Optional<NotificationQueue> optional = notificationQueueRepository.findById(id);
        if (optional.isPresent()) {
            NotificationQueue notification = optional.get();
            notification.setStatus(status);
            
            if (status == NotificationStatus.PROCESSING) {
                notification.markAsProcessing();
            } else if (status == NotificationStatus.SENT) {
                notification.markAsSent();
            }
            
            // Update legacy notification status
            updateLegacyNotificationStatus(notification);
            
            return notificationQueueRepository.save(notification);
        }
        return null;
    }

    /**
     * Mark notification as failed
     */
    public NotificationQueue markNotificationAsFailed(Integer id, String errorMessage) {
        Optional<NotificationQueue> optional = notificationQueueRepository.findById(id);
        if (optional.isPresent()) {
            NotificationQueue notification = optional.get();
            notification.markAsFailed(errorMessage);
            
            // Update legacy notification status
            updateLegacyNotificationStatus(notification);
            
            return notificationQueueRepository.save(notification);
        }
        return null;
    }

    /**
     * Retry a failed notification
     */
    public NotificationQueue retryNotification(Integer id) {
        Optional<NotificationQueue> optional = notificationQueueRepository.findById(id);
        if (optional.isPresent()) {
            NotificationQueue notification = optional.get();
            
            if (!notification.canRetry()) {
                throw new RuntimeException("Notification cannot be retried - max retries exceeded");
            }
            
            notification.incrementRetry();
            
            // Update legacy notification status
            updateLegacyNotificationStatus(notification);
            
            return notificationQueueRepository.save(notification);
        }
        return null;
    }

    /**
     * Cancel a notification
     */
    public NotificationQueue cancelNotification(Integer id) {
        Optional<NotificationQueue> optional = notificationQueueRepository.findById(id);
        if (optional.isPresent()) {
            NotificationQueue notification = optional.get();
            
            if (notification.getStatus() == NotificationStatus.SENT) {
                throw new RuntimeException("Cannot cancel notification that has already been sent");
            }
            
            notification.cancel();
            
            // Update legacy notification status
            updateLegacyNotificationStatus(notification);
            
            return notificationQueueRepository.save(notification);
        }
        return null;
    }

    /**
     * Get queue statistics
     */
    @Transactional
    public Map<String, Long> getQueueStatistics() {
        List<Object[]> results = notificationQueueRepository.getNotificationStatistics();
        return results.stream().collect(
            java.util.stream.Collectors.toMap(
                row -> ((NotificationStatus) row[0]).toString(),
                row -> (Long) row[1]
            )
        );
    }

    /**
     * Get queue health status
     */
    public String getQueueHealthStatus() {
        try {
            long pendingCount = notificationQueueRepository.countByStatus(NotificationStatus.PENDING);
            long processingCount = notificationQueueRepository.countByStatus(NotificationStatus.PROCESSING);
            long failedCount = notificationQueueRepository.countByStatus(NotificationStatus.FAILED);
            long sentCount = notificationQueueRepository.countByStatus(NotificationStatus.SENT);
            
            return String.format("Queue Health - Pending: %d, Processing: %d, Failed: %d, Sent: %d", 
                               pendingCount, processingCount, failedCount, sentCount);
        } catch (Exception e) {
            logger.error("Error getting queue health status: {}", e.getMessage());
            return "Error retrieving queue health: " + e.getMessage();
        }
    }

    /**
     * Reset stale processing notifications
     */
    public int resetStaleProcessingNotifications(int staleMinutes) {
        Date cutoffTime = new Date(System.currentTimeMillis() - (staleMinutes * 60 * 1000));
        return notificationQueueRepository.resetStaleProcessingNotifications(
            NotificationStatus.PROCESSING, 
            NotificationStatus.PENDING, 
            cutoffTime
        );
    }

    /**
     * Clean up old notifications
     */
    public int cleanupOldNotifications(int daysToKeep) {
        Date cutoffDate = new Date(System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000));
        List<NotificationStatus> completedStatuses = List.of(
            NotificationStatus.SENT, 
            NotificationStatus.FAILED_PERMANENTLY, 
            NotificationStatus.CANCELLED
        );
        return notificationQueueRepository.deleteOldNotifications(completedStatuses, cutoffDate);
    }
}
