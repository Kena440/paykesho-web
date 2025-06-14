package org.paykesho.notificationservice.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Notification;
import org.commons.enums.NotificationType;
import org.paykesho.notificationservice.notifications.repository.NotificationsRepository;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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


    public static String trimString(String input) {
        if (input != null && input.length() > MAX_MESSAGE_LENGTH) {
            return input.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return input;
    }

    @Transactional
    public boolean sendNotification(String recipient, String message, String notificationType) {
        if (notificationType == null || recipient == null || message == null) {
            logger.error("Invalid input: recipient, message, or notification type cannot be null.");
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

    public boolean sendSms(String phoneNumber, String message) {
        try {
            String trimmedMessage = trimString(message);
            String encodedMessage = URLEncoder.encode(trimmedMessage, StandardCharsets.UTF_8);
            String apiUrl = String.format(
                    "https://www.smszambia.com/smsservice/httpapi?username=%s&password=%s&msg=%s&sender_id=%s&phone=%s",
                    applicationProperties.getUsername(), applicationProperties.getPassword(), encodedMessage, applicationProperties.getSenderId(), phoneNumber
            );
            String response = restTemplate.getForObject(apiUrl, String.class);
            boolean ok = response != null && response.toLowerCase().contains("ok");
            if (!ok) logger.error("SMS API response: {}", response);
            return ok;
        } catch (Exception e) {
            logger.error("Error sending SMS: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendEmail(String recipient, String subject, String message) {
        String trimmedMessage = trimString(message);
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", applicationProperties.getSmtpServer());
        props.put("mail.smtp.port", String.valueOf(applicationProperties.getSmtpPort()));
        props.put("mail.smtp.ssl.trust", applicationProperties.getSmtpServer());
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(
                        applicationProperties.getSmtpUser(),
                        applicationProperties.getSmtpPassword()
                );
            }
        });
        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(
                    applicationProperties.getFromEmail(),
                    applicationProperties.getFromName()
            ));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            mimeMessage.setSubject(subject);
            mimeMessage.setContent(trimmedMessage, "text/html; charset=utf-8");
            Transport.send(mimeMessage);
            return true;
        } catch (Exception e) {
            logger.error("Email could not be sent. Error: {}", e.getMessage());
            return false;
        }
    }
}
