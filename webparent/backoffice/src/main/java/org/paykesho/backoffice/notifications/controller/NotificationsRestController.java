package org.paykesho.backoffice.notifications.controller;

import org.paykesho.backoffice.notifications.dto.NotificationRequest;
import org.paykesho.backoffice.notifications.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/rest")
public class NotificationsRestController {

    private final NotificationService notificationService;

    public NotificationsRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public String sendNotification(@RequestBody NotificationRequest notificationRequest, @RequestHeader("apiKey") String apiKey) {
        boolean result = notificationService.sendNotification(notificationRequest.getRecipient(), notificationRequest.getMessage(), notificationRequest.getNotification_type());
        return result ? "Notification sent successfully" : "Failed to send notification";
    }
}
