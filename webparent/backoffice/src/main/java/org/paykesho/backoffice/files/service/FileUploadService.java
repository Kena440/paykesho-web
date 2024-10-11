package org.paykesho.backoffice.files.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.paykesho.backoffice.files.repository.FileUploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Date;


@Service
@Transactional
public class FileUploadService {

    private final RestTemplate restTemplate;
    private final FileUploadRepository fileUploadRepository;
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @Autowired
    public FileUploadService(RestTemplate restTemplate, FileUploadRepository fileUploadRepository) {
        this.restTemplate = restTemplate;
        this.fileUploadRepository = fileUploadRepository;
    }

    @Transactional
    public boolean uploadFile(MultipartFile file, FileType fileType, int userId, String description) {
        String apiKey = "F39E25A3B2A446F98E87A95723AE4";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("API-Key", apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        File tempFile = null;

        try {
            tempFile = convertMultipartToFile(file);
            body.add("file", new FileSystemResource(tempFile));
            body.add("fileType", fileType.toString());
            body.add("userId", userId);
            body.add("description", description);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange("http://files.paykesho.com/uploads-api", HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                int fileId = jsonResponse.get("file_id").asInt();
                FileUpload uploadedFile = new FileUpload();
                uploadedFile.setFileName(file.getOriginalFilename());
                uploadedFile.setFileType(fileType);
                uploadedFile.setFilePath(tempFile.getAbsolutePath());
                uploadedFile.setCreatedAt(new Date());
                uploadedFile.setRemoteFileId(fileId);
                fileUploadRepository.save(uploadedFile);
                return true;
            } else {
                logger.error("File upload failed with status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return false;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    @Transactional
    public String getFileAccessDetails(int fileId) {
        String apiKey = "F39E25A3B2A446F98E87A95723AE4";

        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "http://files.paykesho.com/access-api/" + fileId,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                logger.error("Failed to retrieve access details for file ID {}: {}", fileId, response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Error retrieving access details for file ID {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    private File convertMultipartToFile(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(tempFile);
            return tempFile;
        } catch (IOException e) {
            logger.error("Failed to convert MultipartFile to File: {}", e.getMessage());
            throw new RuntimeException("Could not convert file", e);
        }
    }
}
