package org.paykesho.backoffice.notifications.service;

import org.commons.entity.Notification;
import org.commons.enums.NotificationType;
import org.paykesho.backoffice.notifications.dto.NotificationRequest;
import org.paykesho.backoffice.notifications.repository.NotificationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String API_KEY = "F39E25A3B2A446F98E87A95723AE4";
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final String NOTIFICATION_API_URL = "https://notifications.paykesho.com/notifications-api";

    private final RestTemplate restTemplate;
    private final NotificationsRepository notificationRepository;

    @Autowired
    public NotificationService(RestTemplate restTemplate, NotificationsRepository notificationRepository) {
        this.restTemplate = restTemplate;
        this.notificationRepository = notificationRepository;
    }

    public static String trimString(String input) {
        if (input != null && input.length() > MAX_MESSAGE_LENGTH) {
            return input.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return input;
    }

    @Transactional
    public boolean sendNotification(String recipient, String message, String notificationType) {
        // Validate inputs
        if (recipient == null || message == null || notificationType == null) {
            logger.error("Invalid input: recipient, message, or notification type cannot be null.");
            return false;
        }

        Notification notification = new Notification();
        notification.setReference(UUID.randomUUID().toString());
        try {
            notification.setNotificationType(NotificationType.valueOf(notificationType));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid notification type: {}", notificationType);
            return false;
        }
        notification.setRecipient(recipient);
        notification.setMessage(trimString(message));
        notification.setStatus("pending");

        try {
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error saving notification to database: {}", e.getMessage());
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("API-Key", API_KEY);

        NotificationRequest requestBody = new NotificationRequest(recipient, notification.getMessage(), notificationType);

        // Log message length
        logger.info("Message length for recipient {}: {}", recipient, message.length());

        HttpEntity<NotificationRequest> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(NOTIFICATION_API_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                notification.setStatus("sent");
            } else {
                notification.setStatus("failed");
                logger.error("Failed to send notification. Response: {}", response.getBody());
            }

            notificationRepository.save(notification); // Save notification status

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage());
            notification.setStatus("failed");
            try {
                notificationRepository.save(notification); // Save failed notification status
            } catch (Exception saveException) {
                logger.error("Error saving failed notification status to database: {}", saveException.getMessage());
            }
            return false;
        }
    }
}
