package org.paykesho.notificationservice.notifications.controller;


import lombok.RequiredArgsConstructor;
import org.commons.dto.NotificationRequestDTO;
import org.paykesho.notificationservice.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationsRestController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequestDTO notificationRequestDTO) {
        boolean result = notificationService.sendNotification(
                notificationRequestDTO.getRecipient(),
                notificationRequestDTO.getMessage(),
                notificationRequestDTO.getNotificationType()
        );
        if (result) {
            return ResponseEntity.ok("Notification sent successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send notification");
        }
    }
}
