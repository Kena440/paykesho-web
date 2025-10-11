package org.paykesho.fileservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageProperties {
    
    private String uploadDir = "uploads";
    private long maxFileSize = 5242880; // 5MB in bytes
    private String baseUrl = "http://localhost:8086";
    
    public String getFullPath(String subDirectory) {
        return uploadDir + "/" + subDirectory;
    }
}

