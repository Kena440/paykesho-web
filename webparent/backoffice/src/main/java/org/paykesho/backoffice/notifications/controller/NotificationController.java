package org.paykesho.backoffice.notifications.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationPreference;
import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.paykesho.backoffice.notification.repository.NotificationRequestRepository;
import org.paykesho.backoffice.notifications.service.NotificationPreferenceService;
import org.commons.utils.PaykeshoError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRequestRepository notificationRequestRepository;
    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping("")
    public String listNotifications(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "recipient", required = false) String recipient,
            Model model) {
        
        try {
            int pageSize = 20;
            Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
            
            NotificationStatus statusEnum = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusEnum = NotificationStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status parameter: {}", status);
                }
            }
            
            NotificationType typeEnum = null;
            if (type != null && !type.isEmpty()) {
                try {
                    typeEnum = NotificationType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid type parameter: {}", type);
                }
            }
            
            Page<NotificationRequest> notifications;
            if (statusEnum != null && typeEnum != null) {
                notifications = notificationRequestRepository.findByStatusAndNotificationType(statusEnum, typeEnum, pageable);
            } else if (statusEnum != null) {
                notifications = notificationRequestRepository.findByStatus(statusEnum, pageable);
            } else if (typeEnum != null) {
                notifications = notificationRequestRepository.findByNotificationType(typeEnum, pageable);
            } else if (recipient != null && !recipient.isEmpty()) {
                List<NotificationRequest> all = notificationRequestRepository.findByRecipientEmail(recipient);
                // Manual pagination for recipient filter
                int start = (page - 1) * pageSize;
                int end = Math.min(start + pageSize, all.size());
                List<NotificationRequest> pageContent = start < all.size() ? all.subList(Math.min(start, all.size()), end) : List.of();
                notifications = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, all.size());
            } else {
                notifications = notificationRequestRepository.findAll(pageable);
            }
            
            model.addAttribute("notifications", notifications);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", notifications.getTotalPages());
            model.addAttribute("statusFilter", status);
            model.addAttribute("typeFilter", type);
            model.addAttribute("recipientFilter", recipient);
            model.addAttribute("pageTitle", "Notification Tracking");
            
            // Statistics
            long totalNotifications = notificationRequestRepository.count();
            long pendingCount = notificationRequestRepository.countByStatus(NotificationStatus.PENDING);
            long sentCount = notificationRequestRepository.countByStatus(NotificationStatus.SENT);
            long failedCount = notificationRequestRepository.countByStatus(NotificationStatus.FAILED);
            
            model.addAttribute("totalNotifications", totalNotifications);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("sentCount", sentCount);
            model.addAttribute("failedCount", failedCount);
            
        } catch (Exception e) {
            log.error("Error loading notifications: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to load notifications: " + e.getMessage());
        }
        
        return "notifications/list";
    }

    @GetMapping("/preferences")
    public String listPreferences(
            @RequestParam(value = "email", required = false) String email,
            Model model) {
        
        try {
            List<NotificationPreference> preferences;
            if (email != null && !email.isEmpty()) {
                preferences = notificationPreferenceService.getUserPreferences(email);
            } else {
                preferences = List.of();
            }
            
            model.addAttribute("preferences", preferences);
            model.addAttribute("emailFilter", email);
            model.addAttribute("pageTitle", "Notification Preferences");
            
        } catch (Exception e) {
            log.error("Error loading notification preferences: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to load preferences: " + e.getMessage());
        }
        
        return "notifications/preferences";
    }

    @PostMapping("/preferences/update")
    public String updatePreference(
            @RequestParam("userEmail") String userEmail,
            @RequestParam("notificationType") String notificationType,
            @RequestParam("category") String category,
            @RequestParam("enabled") boolean enabled,
            RedirectAttributes redirectAttributes) {
        
        try {
            NotificationType type = NotificationType.valueOf(notificationType.toUpperCase());
            notificationPreferenceService.updatePreference(userEmail, type, category, enabled);
            redirectAttributes.addFlashAttribute("message", "Preference updated successfully");
        } catch (Exception e) {
            log.error("Error updating preference: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update preference: " + e.getMessage());
        }
        
        return "redirect:/notifications/preferences?email=" + userEmail;
    }

    @GetMapping("/view/{id}")
    public String viewNotification(@PathVariable("id") Long id, Model model) {
        try {
            NotificationRequest notification = notificationRequestRepository.findById(id)
                .orElseThrow(() -> new PaykeshoError("Notification not found"));
            
            model.addAttribute("notification", notification);
            model.addAttribute("pageTitle", "Notification Details");
            
        } catch (Exception e) {
            log.error("Error loading notification: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to load notification: " + e.getMessage());
            return "redirect:/notifications";
        }
        
        return "notifications/view";
    }
}

