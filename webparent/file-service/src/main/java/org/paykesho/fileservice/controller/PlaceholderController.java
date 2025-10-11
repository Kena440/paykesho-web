package org.paykesho.fileservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.commons.enums.FileType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Controller for serving placeholder/default images when actual files are missing
 */
@RestController
@RequestMapping("/placeholders")
@Slf4j
@CrossOrigin
public class PlaceholderController {
    
    @GetMapping("/logo")
    public ResponseEntity<byte[]> getDefaultLogo() {
        return getPlaceholderImage("placeholders/default-logo.png", "logo");
    }
    
    @GetMapping("/photo")
    public ResponseEntity<byte[]> getDefaultPhoto() {
        return getPlaceholderImage("placeholders/default-avatar.png", "photo");
    }
    
    @GetMapping("/payslip")
    public ResponseEntity<byte[]> getDefaultPayslip() {
        return getPlaceholderImage("placeholders/default-document.png", "payslip");
    }
    
    @GetMapping("/{fileType}")
    public ResponseEntity<byte[]> getPlaceholderByType(@PathVariable String fileType) {
        try {
            FileType type = FileType.valueOf(fileType.toLowerCase());
            switch (type) {
                case logo:
                    return getDefaultLogo();
                case photo:
                    return getDefaultPhoto();
                case payslip:
                    return getDefaultPayslip();
                default:
                    return getPlaceholderImage("placeholders/default-file.png", "file");
            }
        } catch (IllegalArgumentException e) {
            return getPlaceholderImage("placeholders/default-file.png", "file");
        }
    }
    
    private ResponseEntity<byte[]> getPlaceholderImage(String resourcePath, String type) {
        try {
            // Try to load custom placeholder from resources
            Resource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                byte[] imageBytes = resource.getInputStream().readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                        .body(imageBytes);
            }
        } catch (IOException e) {
            log.warn("Failed to load placeholder image for {}: {}", type, e.getMessage());
        }
        
        // Return SVG placeholder as fallback
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(generateSvgPlaceholder(type));
    }
    
    private byte[] generateSvgPlaceholder(String type) {
        String icon;
        String label;
        String color;
        
        switch (type.toLowerCase()) {
            case "logo":
                icon = "M12 2L2 7v10c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-10-5z";
                label = "Logo";
                color = "#4F46E5";
                break;
            case "photo":
                icon = "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
                label = "Photo";
                color = "#10B981";
                break;
            case "payslip":
                icon = "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zM6 20V4h7v5h5v11H6z";
                label = "Document";
                color = "#F59E0B";
                break;
            default:
                icon = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z";
                label = "File";
                color = "#6B7280";
                break;
        }
        
        String svg = String.format(
            "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200' viewBox='0 0 200 200'>" +
            "<rect width='200' height='200' fill='#F3F4F6'/>" +
            "<g transform='translate(100, 80)'>" +
            "<svg width='48' height='48' viewBox='0 0 24 24' x='-24' y='0'>" +
            "<path fill='%s' d='%s'/>" +
            "</svg>" +
            "</g>" +
            "<text x='100' y='150' text-anchor='middle' font-family='Arial, sans-serif' font-size='16' fill='%s'>%s Missing</text>" +
            "</svg>",
            color, icon, color, label
        );
        
        return svg.getBytes();
    }
}

