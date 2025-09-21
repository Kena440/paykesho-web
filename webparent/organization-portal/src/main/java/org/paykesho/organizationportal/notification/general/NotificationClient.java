package org.paykesho.organizationportal.notification.general;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NotificationClient {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public boolean sendNotification(String recipient, String message, String notificationType) {
        Map<String, String> body = new HashMap<>();
        body.put("recipient", recipient);
        body.put("message", message);
        body.put("notificationType", notificationType);
        try {
            log.info("Sending notification to service: {} with body: {}", notificationServiceUrl + "/api/notifications/send", body);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send", body, Boolean.class
            );
            log.info("Notification service response: {}", response.getBody());
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage(), e);
            return false;
        }
    }
}