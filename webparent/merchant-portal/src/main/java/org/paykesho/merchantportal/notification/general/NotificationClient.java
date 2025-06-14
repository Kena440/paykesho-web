package org.paykesho.merchantportal.notification.general;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send", body, Boolean.class
            );
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            // Optionally log error
            return false;
        }
    }
}