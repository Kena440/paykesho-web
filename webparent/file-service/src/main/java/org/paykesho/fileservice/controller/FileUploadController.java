package org.paykesho.fileservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.commons.dto.FileUploadResponse;
import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.paykesho.fileservice.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@CrossOrigin
public class FileUploadController {
    
    private final FileStorageService fileStorageService;
    
    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    
    @PostMapping("/uploads-api")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("file_type") String fileTypeStr,
            @RequestParam("user_uuid") String userUuid,
            @RequestParam(value = "description", required = false) String description) {
        
        try {
            log.info("Received file upload request: filename={}, fileType={}, userUuid={}", 
                    file.getOriginalFilename(), fileTypeStr, userUuid);
            
            // Validate required parameters
            if (file.isEmpty()) {
                log.warn("Empty file uploaded");
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.error("Missing required field: file"));
            }
            
            if (userUuid == null || userUuid.isEmpty()) {
                log.warn("Missing user_uuid parameter");
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.error("Missing required field: user_uuid"));
            }
            
            // Parse file type
            FileType fileType;
            try {
                fileType = FileType.valueOf(fileTypeStr.toLowerCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid file type: {}", fileTypeStr);
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.error("Unsupported file type. Allowed: logo, payslip, photo"));
            }
            
            // Store file using UUID
            FileUpload fileUpload = fileStorageService.storeFileByUuid(file, fileType, userUuid, description);
            
            // Return success response
            FileUploadResponse response = FileUploadResponse.success(
                    fileUpload.getId(), 
                    fileUpload.getFileName()
            );
            
            log.info("File uploaded successfully: id={}, filename={}, userUuid={}", 
                    fileUpload.getId(), fileUpload.getFileName(), userUuid);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error during file upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FileUploadResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }
}

