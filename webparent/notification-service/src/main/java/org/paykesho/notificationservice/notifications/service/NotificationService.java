package org.paykesho.notificationservice.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Notification;
import org.commons.enums.NotificationType;
import org.paykesho.notificationservice.notifications.repository.NotificationsRepository;
import org.paykesho.notificationservice.utils.EmailTemplateService;
import org.paykesho.notificationservice.utils.general.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
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

    public static String trimString(String input) {
        if (input != null && input.length() > MAX_MESSAGE_LENGTH) {
            return input.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return input;
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

        // Validate notification type
        try {
            NotificationType.valueOf(notificationType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid notification type: {}", notificationType);
            return false;
        }

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
            notification.setNotificationType(NotificationType.valueOf(notificationType));
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
            logger.info("Sending email to: {} with subject: {}", recipient, subject);
            logger.info("Email message length: {} characters", message != null ? message.length() : 0);

            // Don't trim email content as it needs to be much longer than 200 characters
            String emailContent = message; // Use full message for emails
            logger.info("Email content length: {} characters", emailContent != null ? emailContent.length() : 0);

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", applicationProperties.getSmtpServer());
            props.put("mail.smtp.port", String.valueOf(applicationProperties.getSmtpPort()));
            props.put("mail.smtp.ssl.trust", applicationProperties.getSmtpServer());

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
            mimeMessage.setSubject(subject);
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
}
