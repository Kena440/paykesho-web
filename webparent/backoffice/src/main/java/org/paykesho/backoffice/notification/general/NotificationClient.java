package org.paykesho.backoffice.notification.general;

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
    private static final long CIRCUIT_TIMEOUT = 60000; // 1 minute
    private static final int MAX_RETRIES = 3;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${notification.service.url}")
    private String notificationServiceUrl;
    // Simple circuit breaker state
    private volatile boolean circuitOpen = false;
    private volatile long lastFailureTime = 0;

    public boolean sendNotification(String recipient, String message, String notificationType) {
        // Check circuit breaker
        if (isCircuitOpen()) {
            log.warn("Circuit breaker is OPEN for notification service. Skipping notification to {}", recipient);
            return false;
        }

        Map<String, String> body = new HashMap<>();
        body.put("recipient", recipient);
        body.put("message", message);
        body.put("notificationType", notificationType);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Sending notification to service (attempt {}): {} with body: {}",
                        attempt, notificationServiceUrl + "/api/notifications/send", body);

                ResponseEntity<Boolean> response = restTemplate.postForEntity(
                        notificationServiceUrl + "/api/notifications/send", body, Boolean.class
                );

                log.info("Notification service response: {}", response.getBody());

                // Reset circuit breaker on success
                if (Boolean.TRUE.equals(response.getBody())) {
                    resetCircuitBreaker();
                    return true;
                }

                return false;

            } catch (Exception e) {
                log.error("Error sending notification (attempt {}): {}", attempt, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    // Open circuit breaker after max retries
                    openCircuitBreaker();

                    if (e.getMessage() != null && e.getMessage().contains("UnknownHostException")) {
                        log.warn("Notification service is not available. Please ensure the notification service is running on {}", notificationServiceUrl);
                    }
                    return false;
                }

                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    private boolean isCircuitOpen() {
        if (circuitOpen && (System.currentTimeMillis() - lastFailureTime) > CIRCUIT_TIMEOUT) {
            log.info("Circuit breaker timeout expired. Attempting to close circuit.");
            circuitOpen = false;
        }
        return circuitOpen;
    }

    private void openCircuitBreaker() {
        circuitOpen = true;
        lastFailureTime = System.currentTimeMillis();
        log.warn("Circuit breaker OPENED for notification service due to repeated failures");
    }

    private void resetCircuitBreaker() {
        if (circuitOpen) {
            circuitOpen = false;
            lastFailureTime = 0;
            log.info("Circuit breaker CLOSED for notification service - service is healthy");
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
            log.info("Sending merchant admin welcome email to service: {} with body: {}",
                    notificationServiceUrl + "/api/notifications/send-merchant-admin-welcome", body);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send-merchant-admin-welcome", body, Boolean.class
            );
            log.info("Merchant admin welcome email service response: {}", response.getBody());
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("Error sending merchant admin welcome email: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("UnknownHostException")) {
                log.warn("Notification service is not available. Please ensure the notification service is running on {}", notificationServiceUrl);
            }
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
            log.info("Sending organization admin welcome email to service: {} with body: {}",
                    notificationServiceUrl + "/api/notifications/send-organization-admin-welcome", body);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send-organization-admin-welcome", body, Boolean.class
            );
            log.info("Organization admin welcome email service response: {}", response.getBody());
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("Error sending organization admin welcome email: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("UnknownHostException")) {
                log.warn("Notification service is not available. Please ensure the notification service is running on {}", notificationServiceUrl);
            }
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String userName, String userEmail, String resetUrl) {
        Map<String, String> body = new HashMap<>();
        body.put("userName", userName);
        body.put("userEmail", userEmail);
        body.put("resetUrl", resetUrl);
        try {
            log.info("Sending password reset email to service: {} with body: {}",
                    notificationServiceUrl + "/api/notifications/send-password-reset", body);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send-password-reset", body, Boolean.class
            );
            log.info("Password reset email service response: {}", response.getBody());
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("UnknownHostException")) {
                log.warn("Notification service is not available. Please ensure the notification service is running on {}", notificationServiceUrl);
            }
            return false;
        }
    }
}