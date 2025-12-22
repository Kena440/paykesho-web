package org.paykesho.backoffice.notifications.repository;

import org.commons.entity.NotificationPreference;
import org.commons.enums.NotificationType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends CrudRepository<NotificationPreference, Integer> {
    
    /**
     * Find preference by user email, notification type, and category
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.userEmail = :userEmail " +
           "AND np.notificationType = :notificationType AND np.category = :category")
    Optional<NotificationPreference> findByUserEmailAndTypeAndCategory(
        @Param("userEmail") String userEmail,
        @Param("notificationType") NotificationType notificationType,
        @Param("category") String category
    );

    /**
     * Find all preferences for a user
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.userEmail = :userEmail")
    List<NotificationPreference> findByUserEmail(@Param("userEmail") String userEmail);

    /**
     * Check if notification is enabled for user
     */
    @Query("SELECT COALESCE(np.enabled, true) FROM NotificationPreference np " +
           "WHERE np.userEmail = :userEmail AND np.notificationType = :notificationType " +
           "AND np.category = :category")
    boolean isNotificationEnabled(@Param("userEmail") String userEmail,
                                   @Param("notificationType") NotificationType notificationType,
                                   @Param("category") String category);
}

