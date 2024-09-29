package org.commons.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.enums.NotificationType;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @Column(name = "recipient", length = 128)
    private String recipient;
    @Column(name = "message", length = 128)
    private String message;
    @Column(nullable = false)
    private String status;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50)
    private NotificationType notificationType;
    @Column(name = "created_at", length = 128)
    private Date createdAt;
}
