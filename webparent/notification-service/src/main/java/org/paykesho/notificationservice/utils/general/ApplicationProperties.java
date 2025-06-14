package org.paykesho.notificationservice.utils.general;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.configs.values")
public class ApplicationProperties {
    private String smtpServer;
    private String smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String smtpUser;
    private String fromEmail;
    private String fromName;

    private String username;
    private String password;
    private String senderId;
}
