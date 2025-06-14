package org.commons.dto;

import lombok.Data;

@Data
public class NotificationRequestDTO {
    private String recipient;
    private String message;
    private String notificationType;
}

