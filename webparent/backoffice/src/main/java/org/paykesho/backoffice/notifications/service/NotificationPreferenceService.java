package org.paykesho.backoffice.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.NotificationPreference;
import org.commons.enums.NotificationType;
import org.paykesho.backoffice.notifications.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    /**
     * Check if a notification should be sent based on user preferences
     * Returns true if notification should be sent, false if user has opted out
     */
    public boolean shouldSendNotification(String userEmail, NotificationType notificationType, String category) {
        try {
            Optional<NotificationPreference> preference = notificationPreferenceRepository
                .findByUserEmailAndTypeAndCategory(userEmail, notificationType, category);
            
            // If no preference exists, default to enabled (send notification)
            if (preference.isEmpty()) {
                return true;
            }
            
            return preference.get().isEnabled();
        } catch (Exception e) {
            log.error("Error checking notification preference for {}: {}", userEmail, e.getMessage(), e);
            // On error, default to sending notification
            return true;
        }
    }

    /**
     * Get all preferences for a user
     */
    public List<NotificationPreference> getUserPreferences(String userEmail) {
        return notificationPreferenceRepository.findByUserEmail(userEmail);
    }

    /**
     * Update or create notification preference
     */
    @Transactional
    public NotificationPreference updatePreference(String userEmail, NotificationType notificationType, 
                                                   String category, boolean enabled) {
        Optional<NotificationPreference> existing = notificationPreferenceRepository
            .findByUserEmailAndTypeAndCategory(userEmail, notificationType, category);
        
        NotificationPreference preference;
        if (existing.isPresent()) {
            preference = existing.get();
            preference.setEnabled(enabled);
            preference.setUpdatedAt(new Date());
        } else {
            preference = new NotificationPreference();
            preference.setUserEmail(userEmail);
            preference.setNotificationType(notificationType);
            preference.setCategory(category);
            preference.setEnabled(enabled);
        }
        
        return notificationPreferenceRepository.save(preference);
    }
}

