package org.paykesho.clientportal.notifications.controller;


import org.paykesho.clientportal.notifications.dto.NotificationRequest;
import org.paykesho.clientportal.notifications.service.NotificationService;
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
        boolean result = notificationService.sendNotification(notificationRequest.getRecipient(), notificationRequest.getMessage(), notificationRequest.getNotificationType());
        return result ? "Notification sent successfully" : "Failed to send notification";
    }
}
