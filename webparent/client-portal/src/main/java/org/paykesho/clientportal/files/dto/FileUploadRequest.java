package org.paykesho.clientportal.files.dto;

import lombok.Data;
import org.commons.enums.FileType;

@Data
public class FileUploadRequest {
    private String filePath; // This might need to be adjusted based on how you send the file
    private FileType fileType;
    private int userId;
    private String description;

    // Constructors, Getters, and Setters
    public FileUploadRequest(String filePath, FileType fileType, int userId, String description) {
        this.filePath = filePath;
        this.fileType = fileType;
        this.userId = userId;
        this.description = description;
    }

}

