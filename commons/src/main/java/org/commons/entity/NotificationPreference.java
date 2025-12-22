package org.commons.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commons.enums.NotificationType;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity for storing user notification preferences
 * Allows users to opt-out of specific notification types
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_pref_user", columnList = "user_email"),
    @Index(name = "idx_notification_pref_type", columnList = "notification_type")
})
public class NotificationPreference extends IdBasedEntity {

    /**
     * User's email address (can be client, admin, organization admin, merchant admin)
     */
    @Column(name = "user_email", length = 128, nullable = false)
    private String userEmail;

    /**
     * Type of notification (EMAIL, SMS, or both)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20, nullable = false)
    private NotificationType notificationType;

    /**
     * Notification category (REGISTRATION, VOUCHER, REPAYMENT, etc.)
     */
    @Column(name = "category", length = 50, nullable = false)
    private String category;

    /**
     * Whether this notification type is enabled for this user
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * When this preference was created
     */
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * When this preference was last updated
     */
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}

