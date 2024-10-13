package org.paykesho.backoffice.files.dto;

import lombok.Data;
import org.commons.enums.FileType;

@Data
public class FileAccessResponse {
    private String fileName;
    private String fileUrl;
    private boolean status;

 }

