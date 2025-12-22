package org.paykesho.fileservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.paykesho.fileservice.config.FileStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@Slf4j
public class FileServeController {
    
    private final FileStorageProperties fileStorageProperties;
    
    public FileServeController(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }
    
    @Autowired
    private PlaceholderController placeholderController;
    
    @GetMapping("/{fileType}/{filename:.+}")
    public ResponseEntity<?> serveFile(
            @PathVariable String fileType,
            @PathVariable String filename) {
        
        try {
            log.info("Serving file: fileType={}, filename={}", fileType, filename);
            
            // Validate fileType to prevent directory traversal
            if (fileType.contains("..") || fileType.contains("/") || fileType.contains("\\")) {
                log.warn("Invalid fileType parameter: {}, returning placeholder", fileType);
                return placeholderController.getPlaceholderByType("logo");
            }
            
            // Validate filename to prevent directory traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Invalid filename parameter: {}, returning placeholder", filename);
                return placeholderController.getPlaceholderByType(fileType);
            }
            
            // Handle both singular and plural forms for backward compatibility
            String actualFileType = fileType;
            if ("logo".equals(fileType)) {
                // Try logos (plural) first, then fallback to logo (singular)
                String logosPath = fileStorageProperties.getFullPath("logos");
                Path logosFilePath = Paths.get(logosPath).resolve(filename).normalize();
                if (Files.exists(logosFilePath)) {
                    actualFileType = "logos";
                    log.debug("Found file using plural form: logos");
                }
            } else if ("logos".equals(fileType)) {
                // Handle reverse case: if request is for 'logos' but file is stored under 'logo'
                String logoPath = fileStorageProperties.getFullPath("logo");
                Path logoFilePath = Paths.get(logoPath).resolve(filename).normalize();
                if (Files.exists(logoFilePath)) {
                    actualFileType = "logo";
                    log.debug("Found file using singular form: logo");
                }
            }
            
            // Use the same path construction as FileStorageService for consistency
            String fullPath = fileStorageProperties.getFullPath(actualFileType);
            Path filePath = Paths.get(fullPath).resolve(filename).normalize();
            
            // Security check: ensure the resolved path is within the upload directory
            Path uploadDir = Paths.get(fileStorageProperties.getUploadDir()).normalize();
            if (!filePath.startsWith(uploadDir)) {
                log.warn("Path traversal attempt detected: {}, returning placeholder", filePath);
                return placeholderController.getPlaceholderByType(fileType);
            }
            
            log.debug("Looking for file at path: {}", filePath.toAbsolutePath());
            
            // Check file existence using Files.exists() first for better debugging
            boolean fileExists = Files.exists(filePath);
            log.debug("File exists check (Files.exists): {}", fileExists);
            
            Resource resource = new UrlResource(filePath.toUri());
            boolean resourceExists = resource.exists();
            boolean resourceReadable = resource.exists() && resource.isReadable();
            
            log.debug("Resource check: exists={}, readable={}", resourceExists, resourceReadable);
            
            if (!fileExists || !resourceExists || !resourceReadable) {
                log.warn("File not found or not readable: fileType={}, filename={}, path={}", 
                        fileType, filename, filePath.toAbsolutePath());
                log.warn("Checks - Files.exists: {}, Resource.exists: {}, Resource.readable: {}", 
                        fileExists, resourceExists, resourceReadable);
                
                // Try to list the directory to see what files are actually there
                try {
                    Path parentDir = filePath.getParent();
                    if (Files.exists(parentDir)) {
                        log.debug("Directory contents: {}", Files.list(parentDir).map(Path::getFileName).toArray());
                    } else {
                        log.warn("Parent directory does not exist: {}", parentDir);
                    }
                } catch (Exception e) {
                    log.debug("Could not list directory contents: {}", e.getMessage());
                }
                
                // Return placeholder instead of 404
                return placeholderController.getPlaceholderByType(fileType);
            }
            
            // Determine content type
            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }
            
            log.info("File served successfully: {}", filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "inline; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error serving file: {}, returning placeholder", filename, e);
            // Return placeholder on any error
            try {
                return placeholderController.getPlaceholderByType(fileType);
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
    
}

