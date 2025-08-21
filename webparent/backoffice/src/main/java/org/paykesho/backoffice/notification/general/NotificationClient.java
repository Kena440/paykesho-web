package org.paykesho.backoffice.notification.general;

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

    public boolean sendMerchantAdminWelcomeEmail(String adminName, String adminEmail, String merchantName, String portalUrl, String temporaryPassword) {
        Map<String, String> body = new HashMap<>();
        body.put("adminName", adminName);
        body.put("adminEmail", adminEmail);
        body.put("merchantName", merchantName);
        body.put("portalUrl", portalUrl);
        body.put("temporaryPassword", temporaryPassword);
        try {
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send-merchant-admin-welcome", body, Boolean.class
            );
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            // Optionally log error
            return false;
        }
    }

    public boolean sendOrganizationAdminWelcomeEmail(String adminName, String adminEmail, String organizationName, String portalUrl, String temporaryPassword) {
        Map<String, String> body = new HashMap<>();
        body.put("adminName", adminName);
        body.put("adminEmail", adminEmail);
        body.put("organizationName", organizationName);
        body.put("portalUrl", portalUrl);
        body.put("temporaryPassword", temporaryPassword);
        try {
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send-organization-admin-welcome", body, Boolean.class
            );
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            // Optionally log error
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String userName, String userEmail, String resetUrl) {
        Map<String, String> body = new HashMap<>();
        body.put("userName", userName);
        body.put("userEmail", userEmail);
        body.put("resetUrl", resetUrl);
        try {
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send-password-reset", body, Boolean.class
            );
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            // Optionally log error
            return false;
        }
    }
}