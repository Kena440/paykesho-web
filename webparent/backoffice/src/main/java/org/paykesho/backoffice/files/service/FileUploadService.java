package org.paykesho.backoffice.files.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.dto.FileUploadResponse;
import org.paykesho.backoffice.files.repository.FileUploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    private final RestTemplate restTemplate;
    private final FileUploadRepository fileUploadRepository;
    
    @Value("${file.service.url:http://localhost:8086}")
    private String fileServiceUrl;

    public FileUploadService(RestTemplate restTemplate, FileUploadRepository fileUploadRepository) {
        this.restTemplate = restTemplate;
        this.fileUploadRepository = fileUploadRepository;
    }

    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, FileType fileType, int userId, String description) {
        return uploadFileInternal(file, fileType, String.valueOf(userId), true, description);
    }
    
    @Transactional
    public FileUploadResponse uploadFileByUuid(MultipartFile file, FileType fileType, String userUuid, String description) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new IllegalArgumentException("User UUID cannot be null or empty");
        }
        return uploadFileInternal(file, fileType, userUuid, false, description);
    }
    
    private FileUploadResponse uploadFileInternal(MultipartFile file, FileType fileType, String userIdentifier, boolean isUserId, String description) {
        String apiKey = "F39E25A3B2A446F98E87A95723AE4";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("API-Key", apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        File tempFile = null;
        FileUploadResponse fileUploadResponse = new FileUploadResponse();

        try {
            tempFile = convertMultipartToFile(file);
            body.add("file", new FileSystemResource(tempFile));
            body.add("file_type", fileType.toString().toLowerCase());
            
            // Add either user_id or user_uuid based on parameter
            if (isUserId) {
                body.add("user_id", userIdentifier);
            } else {
                body.add("user_uuid", userIdentifier);
            }
            
            body.add("description", description);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(fileServiceUrl + "/uploads-api", HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                int fileId = jsonResponse.get("file_id").asInt();
                String message = jsonResponse.get("message").asText();
                String filePath = jsonResponse.get("file_name").asText();
                FileUpload uploadedFile = new FileUpload();
                uploadedFile.setFileName(file.getOriginalFilename());
                uploadedFile.setFileType(fileType);
                uploadedFile.setFilePath(tempFile.getAbsolutePath());
                uploadedFile.setCreatedAt(LocalDateTime.now());
                uploadedFile.setRemoteFileId(fileId);
                
                // Set either userId or userUuid based on what was provided
                if (isUserId) {
                    uploadedFile.setUserId(Integer.parseInt(userIdentifier));
                } else {
                    uploadedFile.setUserUuid(userIdentifier);
                }
                
                fileUploadRepository.save(uploadedFile);

                fileUploadResponse.setMessage(message);
                fileUploadResponse.setStatus(true);
                fileUploadResponse.setFileId(fileId);
                fileUploadResponse.setFilePath(filePath);
                return fileUploadResponse;
            } else {
                logger.error("File upload failed with status: {}", response.getStatusCode());
                fileUploadResponse.setStatus(false);
                return fileUploadResponse;
            }

        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            fileUploadResponse.setStatus(false);
            return fileUploadResponse;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    @Transactional
    public FileAccessResponse getFileAccessDetails(int fileId) {
        String apiKey = "F39E25A3B2A446F98E87A95723AE4";
        String url = fileServiceUrl + "/access-api";

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Integer> requestBody = new HashMap<>();
        requestBody.put("file_id", fileId);

        HttpEntity<Map<String, Integer>> requestEntity = new HttpEntity<>(requestBody, headers);
        FileAccessResponse fileAccessResponse = new FileAccessResponse();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String fileName = jsonResponse.get("file_name").asText();
                String fileUrl = jsonResponse.get("file_url").asText();
                fileAccessResponse.setFileName(fileName);
                fileAccessResponse.setFileUrl(fileUrl);
                fileAccessResponse.setStatus(true);
                return fileAccessResponse;
            } else {
                logger.error("Failed to retrieve access details for file ID {}: {}", fileId, response.getStatusCode());
                fileAccessResponse.setStatus(false);
                return fileAccessResponse;
            }

        } catch (Exception e) {
            logger.error("Error retrieving access details for file ID {}: {}", fileId, e.getMessage());
            fileAccessResponse.setStatus(false);
            return fileAccessResponse;
        }
    }
    
    /**
     * Get file access details with fallback to placeholder
     */
    @Transactional
    public FileAccessResponse getFileAccessDetailsWithFallback(int fileId, FileType fileType) {
        FileAccessResponse response = getFileAccessDetails(fileId);
        
        if (response == null || !response.isStatus()) {
            // Return placeholder URL based on file type
            response = new FileAccessResponse();
            response.setFileName("placeholder");
            response.setFileUrl(getPlaceholderUrl(fileType));
            response.setStatus(true);
            logger.info("Returning placeholder for file ID {} of type {}", fileId, fileType);
        }
        
        return response;
    }
    
    /**
     * Get placeholder URL for missing files
     */
    public String getPlaceholderUrl(FileType fileType) {
        if (fileType == null) {
            return fileServiceUrl + "/placeholders/file";
        }
        return fileServiceUrl + "/placeholders/" + fileType.name().toLowerCase();
    }

    private File convertMultipartToFile(MultipartFile file) {
        try {
            // Generate UUID-based filename to avoid conflicts and improve security
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Create temp file with UUID prefix and original extension
            String uuidFilename = java.util.UUID.randomUUID().toString() + fileExtension;
            File tempFile = File.createTempFile("upload_", "_" + uuidFilename);
            file.transferTo(tempFile);
            
            logger.info("File saved locally with UUID name: {} (original: {})", tempFile.getName(), originalFilename);
            return tempFile;
        } catch (IOException e) {
            logger.error("Failed to convert MultipartFile to File: {}", e.getMessage());
            throw new RuntimeException("Could not convert file: " + e.getMessage(), e);
        }
    }
}
