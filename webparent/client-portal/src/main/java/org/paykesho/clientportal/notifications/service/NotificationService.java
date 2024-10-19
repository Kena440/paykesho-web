package org.paykesho.clientportal.notifications.service;

import org.commons.entity.Notification;
import org.commons.enums.NotificationType;
import org.paykesho.clientportal.notifications.dto.NotificationRequest;
import org.paykesho.clientportal.notifications.repository.NotificationsRepository;
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
    private final RestTemplate restTemplate;
    private final NotificationsRepository notificationRepository;

    @Autowired
    public NotificationService(RestTemplate restTemplate, NotificationsRepository notificationRepository) {
        this.restTemplate = restTemplate;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public boolean sendNotification(String recipient, String message, String notificationType) {
        String apiKey = "F39E25A3B2A446F98E87A95723AE4";
        Notification notification = new Notification();
        notification.setReference(UUID.randomUUID().toString());
        notification.setNotificationType(NotificationType.valueOf(notificationType));
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setStatus("pending");

        try {
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error saving notification to database: {}", e.getMessage());
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("API-Key", apiKey);

        NotificationRequest requestBody = new NotificationRequest(recipient, message, notificationType);

        // Log message length
        logger.info("Message length for recipient {}: {}", recipient, message.length());

        HttpEntity<NotificationRequest> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://notifications.paykesho.com/notifications-api",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                notification.setStatus("sent");
            } else {
                notification.setStatus("failed");
            }

            try {
                notificationRepository.save(notification);
            } catch (Exception e) {
                logger.error("Error saving notification status to database: {}", e.getMessage());
            }

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage());
            notification.setStatus("failed");
            try {
                notificationRepository.save(notification);
            } catch (Exception saveException) {
                logger.error("Error saving failed notification status to database: {}", saveException.getMessage());
            }
            return false;
        }
    }
}
