package org.commons.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    
    private String status;
    private String message;
    private Long file_id;
    private String file_name;
    private String error;
    
    public static FileUploadResponse success(Long fileId, String fileName) {
        return FileUploadResponse.builder()
                .status("success")
                .message("File uploaded successfully")
                .file_id(fileId)
                .file_name(fileName)
                .build();
    }
    
    public static FileUploadResponse error(String errorMessage) {
        return FileUploadResponse.builder()
                .error(errorMessage)
                .build();
    }
}

