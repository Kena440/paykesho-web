package org.commons.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAccessResponse {
    
    private String file_name;
    private String file_url;
    private String error;
    
    public static FileAccessResponse success(String fileName, String fileUrl) {
        return FileAccessResponse.builder()
                .file_name(fileName)
                .file_url(fileUrl)
                .build();
    }
    
    public static FileAccessResponse error(String errorMessage) {
        return FileAccessResponse.builder()
                .error(errorMessage)
                .build();
    }
}

