package org.paykesho.backoffice.files.controller;

import lombok.extern.slf4j.Slf4j;
import org.commons.enums.FileType;
import org.paykesho.backoffice.files.dto.FileUploadResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Secure file upload controller with comprehensive validation
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class SecureFileUploadController {

    // Allowed MIME types for additional security
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    // Dangerous file extensions that should never be allowed
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", "php", "asp", "jsp"
    );
    // Pattern for safe filename validation
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private final FileUploadService fileUploadService;
    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;
    @Value("${file.upload.allowed-types:jpg,jpeg,png,gif,pdf,doc,docx}")
    private String allowedFileTypes;

    public SecureFileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleSecureFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileTypeStr,
            @RequestParam("userUuid") String userUuid,
            @RequestParam("description") String description) {

        Map<String, Object> response = new HashMap<>();

        try {
            // SECURITY CHECK #1: Validate file is not null and not empty
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("error", "No file provided or file is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #2: Validate file size
            if (file.getSize() > maxFileSize) {
                response.put("success", false);
                response.put("error", String.format("File size exceeds maximum allowed size of %d bytes", maxFileSize));
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #3: Validate filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Invalid filename");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #4: Sanitize filename
            String sanitizedFilename = sanitizeFilename(originalFilename);
            if (!SAFE_FILENAME_PATTERN.matcher(sanitizedFilename).matches()) {
                response.put("success", false);
                response.put("error", "Filename contains invalid characters");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #5: Validate file extension
            String fileExtension = getFileExtension(sanitizedFilename).toLowerCase();
            if (DANGEROUS_EXTENSIONS.contains(fileExtension)) {
                response.put("success", false);
                response.put("error", "File type not allowed for security reasons");
                return ResponseEntity.badRequest().body(response);
            }

            Set<String> allowedExtensions = Set.of(allowedFileTypes.split(","));
            if (!allowedExtensions.contains(fileExtension)) {
                response.put("success", false);
                response.put("error", "File type not allowed. Allowed types: " + allowedFileTypes);
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #6: Validate MIME type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                response.put("success", false);
                response.put("error", "Invalid file content type");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #7: Validate file content (basic magic number check)
            if (!isValidFileContent(file, fileExtension)) {
                response.put("success", false);
                response.put("error", "File content does not match file extension");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #8: Validate file type enum
            FileType fileType;
            try {
                fileType = FileType.valueOf(fileTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("error", "Invalid file type specified");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #9: Validate user UUID
            if (userUuid == null || userUuid.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Invalid user UUID");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY CHECK #10: Validate description
            if (description == null || description.trim().length() > 500) {
                response.put("success", false);
                response.put("error", "Description is required and must be less than 500 characters");
                return ResponseEntity.badRequest().body(response);
            }

            // Process the upload using UUID
            FileUploadResponse uploadResponse = fileUploadService.uploadFileByUuid(file, fileType, userUuid, description);

            if (uploadResponse != null && uploadResponse.isStatus()) {
                response.put("success", true);
                response.put("fileId", uploadResponse.getFileId());
                response.put("filePath", uploadResponse.getFilePath());
                response.put("message", "File uploaded successfully");

                log.info("File uploaded successfully: {} by user UUID: {}", sanitizedFilename, userUuid);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "File upload failed");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "File upload failed due to system error");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/access/{fileId}")
    public ResponseEntity<Map<String, Object>> getSecureFileAccess(@PathVariable int fileId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (fileId <= 0) {
                response.put("success", false);
                response.put("error", "Invalid file ID");
                return ResponseEntity.badRequest().body(response);
            }

            var fileAccessResponse = fileUploadService.getFileAccessDetails(fileId);
            if (fileAccessResponse != null) {
                response.put("success", true);
                response.put("accessDetails", fileAccessResponse.getFileUrl());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "File not found or access denied");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error accessing file {}: {}", fileId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to access file");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Security utility methods
    private String sanitizeFilename(String filename) {
        if (filename == null) return "";

        // Remove path traversal attempts
        filename = filename.replaceAll("\\.\\./", "");
        filename = filename.replaceAll("\\.\\.\\\\", "");

        // Remove potentially dangerous characters
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Limit length
        if (filename.length() > 100) {
            String extension = getFileExtension(filename);
            filename = filename.substring(0, 95 - extension.length()) + "." + extension;
        }

        return filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private boolean isValidFileContent(MultipartFile file, String expectedExtension) {
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) return false;

            // Basic magic number validation
            return switch (expectedExtension.toLowerCase()) {
                case "jpg", "jpeg" -> (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8);
                case "png" -> (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 &&
                        fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47);
                case "gif" ->
                        (fileBytes[0] == (byte) 0x47 && fileBytes[1] == (byte) 0x49 && fileBytes[2] == (byte) 0x46);
                case "pdf" -> (fileBytes[0] == (byte) 0x25 && fileBytes[1] == (byte) 0x50 &&
                        fileBytes[2] == (byte) 0x44 && fileBytes[3] == (byte) 0x46);
                default -> true; // Allow other types for now, but log for monitoring
            };
        } catch (IOException e) {
            log.error("Error reading file content for validation: {}", e.getMessage());
            return false;
        }
    }
}
