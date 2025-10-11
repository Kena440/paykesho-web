package org.paykesho.fileservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.commons.dto.FileAccessRequest;
import org.commons.dto.FileAccessResponse;
import org.commons.entity.FileUpload;
import org.paykesho.fileservice.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@Slf4j
@CrossOrigin
public class FileAccessController {
    
    private final FileStorageService fileStorageService;
    
    public FileAccessController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    
    @PostMapping("/access-api")
    public ResponseEntity<FileAccessResponse> getFileAccess(@RequestBody FileAccessRequest request) {
        
        try {
            log.info("Received file access request for file_id: {}", request.getFile_id());
            
            // Validate file_id
            if (request.getFile_id() == null) {
                log.warn("Missing file_id parameter");
                return ResponseEntity.badRequest()
                        .body(FileAccessResponse.error("File ID is required"));
            }
            
            // Get file metadata
            Optional<FileUpload> fileUploadOpt = fileStorageService.getFileMetadata(request.getFile_id());
            
            if (fileUploadOpt.isEmpty()) {
                log.warn("File not found: {}", request.getFile_id());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(FileAccessResponse.error("File not found"));
            }
            
            FileUpload fileUpload = fileUploadOpt.get();
            
            // Generate file URL
            String fileUrl = fileStorageService.generateFileUrl(fileUpload);
            
            // Return response
            FileAccessResponse response = FileAccessResponse.success(
                    fileUpload.getFileName(),
                    fileUrl
            );
            
            log.info("File access successful: id={}, filename={}", 
                    fileUpload.getId(), fileUpload.getFileName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving file access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileAccessResponse.error("Failed to retrieve file access: " + e.getMessage()));
        }
    }
}

