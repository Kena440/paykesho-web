package org.paykesho.backoffice.files.dto;

import lombok.Data;

@Data
public class FileAccessResponse {
    private String fileName;
    private String fileUrl;
    private boolean status;

}

