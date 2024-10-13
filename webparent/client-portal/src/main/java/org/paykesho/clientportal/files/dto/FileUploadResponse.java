package org.paykesho.clientportal.files.dto;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String filePath;
    private boolean status;
    private int fileId;
    private String message;
}

