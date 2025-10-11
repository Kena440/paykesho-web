package org.paykesho.fileservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAccessRequest {
    
    @NotNull(message = "File ID is required")
    private Long file_id;
}

