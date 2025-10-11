package org.paykesho.fileservice.controller;

import lombok.extern.slf4j.Slf4j;
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
@CrossOrigin
public class FileServeController {
    
    private final Path uploadDir = Paths.get("uploads");
    
    @Autowired
    private PlaceholderController placeholderController;
    
    @GetMapping("/{fileType}/{filename:.+}")
    public ResponseEntity<?> serveFile(
            @PathVariable String fileType,
            @PathVariable String filename) {
        
        try {
            log.info("Serving file: fileType={}, filename={}", fileType, filename);
            
            Path filePath = uploadDir.resolve(fileType).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {}, returning placeholder", filePath);
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

