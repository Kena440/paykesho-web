package org.paykesho.backoffice.notifications.service;

import org.springframework.http.HttpEntity;
import org.paykesho.backoffice.notifications.dto.NotificationRequest;
import org.paykesho.backoffice.notifications.repository.NotificationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.commons.entity.Notification;
import org.commons.enums.NotificationType;

import javax.transaction.Transactional;

@Service
@Transactional
public class NotificationService {

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
        notification.setNotificationType(NotificationType.valueOf(notificationType));
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setStatus("pending");

        notificationRepository.save(notification);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        NotificationRequest requestBody = new NotificationRequest(recipient, message, notificationType);

        HttpEntity<NotificationRequest> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "http://paykesho.kaedigital.tech/notifications-api",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                notification.setStatus("sent");
            } else {
                notification.setStatus("failed");
            }

            notificationRepository.save(notification);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
            notification.setStatus("failed");
            notificationRepository.save(notification);
            return false;
        }
    }
}
