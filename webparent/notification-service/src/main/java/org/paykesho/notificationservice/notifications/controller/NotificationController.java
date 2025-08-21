package org.paykesho.notificationservice.notifications.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.paykesho.notificationservice.notifications.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<Boolean> sendNotification(@RequestBody Map<String, String> request) {
        try {
            String recipient = request.get("recipient");
            String message = request.get("message");
            String notificationType = request.get("notificationType");
            
            boolean result = notificationService.sendNotification(recipient, message, notificationType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/send-merchant-admin-welcome")
    public ResponseEntity<Boolean> sendMerchantAdminWelcomeEmail(@RequestBody Map<String, String> request) {
        try {
            String adminName = request.get("adminName");
            String adminEmail = request.get("adminEmail");
            String merchantName = request.get("merchantName");
            String portalUrl = request.get("portalUrl");
            String temporaryPassword = request.get("temporaryPassword");
            
            boolean result = notificationService.sendMerchantAdminWelcomeEmail(
                adminName, adminEmail, merchantName, portalUrl, temporaryPassword
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error sending merchant admin welcome email: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/send-organization-admin-welcome")
    public ResponseEntity<Boolean> sendOrganizationAdminWelcomeEmail(@RequestBody Map<String, String> request) {
        try {
            String adminName = request.get("adminName");
            String adminEmail = request.get("adminEmail");
            String organizationName = request.get("organizationName");
            String portalUrl = request.get("portalUrl");
            String temporaryPassword = request.get("temporaryPassword");
            
            boolean result = notificationService.sendOrganizationAdminWelcomeEmail(
                adminName, adminEmail, organizationName, portalUrl, temporaryPassword
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error sending organization admin welcome email: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/send-password-reset")
    public ResponseEntity<Boolean> sendPasswordResetEmail(@RequestBody Map<String, String> request) {
        try {
            String userName = request.get("userName");
            String userEmail = request.get("userEmail");
            String resetUrl = request.get("resetUrl");
            
            boolean result = notificationService.sendPasswordResetEmail(userName, userEmail, resetUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/send-account-locked")
    public ResponseEntity<Boolean> sendAccountLockedEmail(@RequestBody Map<String, String> request) {
        try {
            String userName = request.get("userName");
            String userEmail = request.get("userEmail");
            String lockTime = request.get("lockTime");
            String unlockTime = request.get("unlockTime");
            
            // Parse the datetime strings (you might want to use a proper date format)
            // For now, using current time as fallback
            java.time.LocalDateTime lockDateTime = java.time.LocalDateTime.now();
            java.time.LocalDateTime unlockDateTime = lockDateTime.plusMinutes(30);
            
            boolean result = notificationService.sendAccountLockedEmail(userName, userEmail, lockDateTime, unlockDateTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error sending account locked email: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/send-custom-templated")
    public ResponseEntity<Boolean> sendCustomTemplatedEmail(@RequestBody Map<String, String> request) {
        try {
            String recipient = request.get("recipient");
            String subject = request.get("subject");
            String greeting = request.get("greeting");
            String content = request.get("content");
            String highlightTitle = request.get("highlightTitle");
            String highlightContent = request.get("highlightContent");
            String warningTitle = request.get("warningTitle");
            String warningContent = request.get("warningContent");
            String actionUrl = request.get("actionUrl");
            String actionText = request.get("actionText");
            
            boolean result = notificationService.sendCustomTemplatedEmail(
                recipient, subject, greeting, content, highlightTitle, highlightContent,
                warningTitle, warningContent, actionUrl, actionText
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error sending custom templated email: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }
}
