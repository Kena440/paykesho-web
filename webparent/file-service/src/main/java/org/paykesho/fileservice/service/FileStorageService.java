package org.paykesho.fileservice.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.paykesho.fileservice.config.FileStorageProperties;
import org.paykesho.fileservice.repository.FileUploadRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    private final FileStorageProperties fileStorageProperties;
    private final FileUploadRepository fileUploadRepository;
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "application/pdf"
    );
    
    public FileStorageService(FileStorageProperties fileStorageProperties, 
                            FileUploadRepository fileUploadRepository) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileUploadRepository = fileUploadRepository;
        initializeStorageDirectories();
    }
    
    private void initializeStorageDirectories() {
        try {
            for (FileType fileType : FileType.values()) {
                Path directory = Paths.get(fileStorageProperties.getFullPath(fileType.name()));
                if (!Files.exists(directory)) {
                    Files.createDirectories(directory);
                    log.info("Created directory: {}", directory.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize storage directories", e);
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }
    
    @Transactional
    public FileUpload storeFile(MultipartFile file, FileType fileType, Integer userId, String description) {
        return storeFileInternal(file, fileType, userId, null, description);
    }
    
    @Transactional
    public FileUpload storeFileByUuid(MultipartFile file, FileType fileType, String userUuid, String description) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new IllegalArgumentException("User UUID cannot be null or empty");
        }
        return storeFileInternal(file, fileType, null, userUuid, description);
    }
    
    private FileUpload storeFileInternal(MultipartFile file, FileType fileType, Integer userId, String userUuid, String description) {
        // Validate file
        validateFile(file);
        
        // Clean filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        String cleanFilename = StringUtils.cleanPath(originalFilename);
        
        try {
            // Generate unique filename with sanitization
            String storedFileName = UUID.randomUUID().toString() + "_" + 
                                  sanitizeFilename(cleanFilename);
            
            // Determine storage path
            Path targetLocation = Paths.get(fileStorageProperties.getFullPath(fileType.name()))
                                      .resolve(storedFileName);
            
            // Store file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Create database record
            FileUpload fileUpload = new FileUpload();
            fileUpload.setFileName(cleanFilename);
            fileUpload.setFilePath(targetLocation.toString());
            fileUpload.setFileType(fileType);
            fileUpload.setUserId(userId);
            fileUpload.setUserUuid(userUuid);
            fileUpload.setDescription(description);
            fileUpload.setFileSize(file.getSize());
            fileUpload.setContentType(file.getContentType());
            fileUpload.setCreatedAt(LocalDateTime.now());
            
            String userIdentifier = userUuid != null ? userUuid : String.valueOf(userId);
            fileUpload.setLogMessage(String.format("File successfully uploaded by user %s", userIdentifier));
            
            FileUpload savedFile = fileUploadRepository.save(fileUpload);
            log.info("File stored successfully: {} with ID: {}", cleanFilename, savedFile.getId());
            
            return savedFile;
            
        } catch (IOException e) {
            log.error("Failed to store file: {}", cleanFilename, e);
            throw new RuntimeException("Failed to store file: " + cleanFilename, e);
        }
    }
    
    public Optional<FileUpload> getFileMetadata(Long fileId) {
        return fileUploadRepository.findById(fileId);
    }
    
    public String generateFileUrl(FileUpload fileUpload) {
        String fileName = Paths.get(fileUpload.getFilePath()).getFileName().toString();
        return String.format("%s/uploads/%s/%s", 
                           fileStorageProperties.getBaseUrl(),
                           fileUpload.getFileType().name(),
                           fileName);
    }
    
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        
        // Check file size
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d bytes", 
                            fileStorageProperties.getMaxFileSize())
            );
        }
        
        // Check content type
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                "Invalid file type. Only JPEG, PNG, and PDF files are allowed"
            );
        }
        
        // Check for path traversal
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence");
        }
    }
    
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "");
    }
}

