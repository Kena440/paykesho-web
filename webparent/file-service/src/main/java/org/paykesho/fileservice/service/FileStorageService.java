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
        if (fileId == null) {
            log.warn("File ID is null");
            return Optional.empty();
        }
        
        Optional<FileUpload> result = fileUploadRepository.findById(fileId);
        if (result.isEmpty()) {
            log.warn("File not found in database for ID: {}", fileId);
            // Additional diagnostic: check if there are any files with similar IDs
            checkNearbyFileIds(fileId);
        } else {
            FileUpload fileUpload = result.get();
            log.debug("File found in database for ID: {}, filename: {}, filePath: {}", 
                     fileId, fileUpload.getFileName(), fileUpload.getFilePath());
            
            // Validate that the file actually exists on disk
            if (!Files.exists(Paths.get(fileUpload.getFilePath()))) {
                log.error("File exists in database but not on disk: ID={}, path={}", 
                         fileId, fileUpload.getFilePath());
            }
        }
        
        return result;
    }
    
    /**
     * Helper method to check for nearby file IDs for diagnostic purposes
     */
    private void checkNearbyFileIds(Long missingId) {
        try {
            log.warn("Checking for nearby file IDs around missing ID: {}", missingId);
            boolean foundNearby = false;
            
            // Check a few IDs around the missing one to see if there's a pattern
            for (long i = Math.max(1, missingId - 5); i <= missingId + 5; i++) {
                if (i != missingId && fileUploadRepository.existsById(i)) {
                    log.warn("Found nearby file ID: {} (missing ID was {})", i, missingId);
                    foundNearby = true;
                }
            }
            
            if (!foundNearby) {
                log.warn("No nearby file IDs found around missing ID: {}", missingId);
            }
        } catch (Exception e) {
            log.warn("Could not check nearby file IDs for diagnostic: {}", e.getMessage());
        }
    }
    
    public String generateFileUrl(FileUpload fileUpload) {
        // Use the filename from the file path (which includes the UUID prefix)
        // to ensure consistency with how the file was actually stored
        Path filePath = Paths.get(fileUpload.getFilePath());
        String fileName = filePath.getFileName().toString();
        
        // Validate that the file path is properly set and the filename exists
        if (fileName == null || fileName.isEmpty()) {
            log.error("Invalid filename in file path for file ID: {}", fileUpload.getId());
            throw new IllegalStateException("Unable to generate file URL: invalid filename in file path");
        }
        
        // Handle the mismatch between stored path and URL generation
        // Files are stored under 'logos' (plural) but FileType enum is 'logo' (singular)
        String fileTypePath = fileUpload.getFileType().name();
        if ("logo".equals(fileTypePath)) {
            // For logo files, check if the actual stored path uses plural form
            String storedPath = fileUpload.getFilePath();
            if (storedPath != null && storedPath.contains("/logos/")) {
                fileTypePath = "logos";
            }
        }
        
        return String.format("%s/uploads/%s/%s", 
                           fileStorageProperties.getBaseUrl(),
                           fileTypePath,
                           fileName);
    }
    
    /**
     * Checks if a file exists both in database and on disk
     * Returns true if both exist, false otherwise
     */
    public boolean validateFileIntegrity(Long fileId) {
        Optional<FileUpload> fileUploadOpt = fileUploadRepository.findById(fileId);
        if (fileUploadOpt.isEmpty()) {
            log.warn("File ID {} not found in database", fileId);
            return false;
        }
        
        FileUpload fileUpload = fileUploadOpt.get();
        boolean existsOnDisk = Files.exists(Paths.get(fileUpload.getFilePath()));
        
        if (!existsOnDisk) {
            log.error("File ID {} exists in database but not on disk: {}", fileId, fileUpload.getFilePath());
        }
        
        return existsOnDisk;
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

