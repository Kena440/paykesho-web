package org.paykesho.backoffice.files.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.commons.utils.StoredFiles;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.dto.FileUploadResponse;
import org.paykesho.backoffice.files.repository.FileUploadRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Stores uploaded files (logos, photos, documents) in the database and serves
 * them only through the authenticated back-office {@code /files/{id}} endpoint.
 * Also opens files clients uploaded in the client portal (payslips), which use
 * the same table.
 *
 * Previously uploads went to the separate file service, which is not deployed
 * and kept files on a container disk that is wiped on restart.
 */
@Slf4j
@Service
@Transactional
public class FileUploadService {

    /** Path, relative to the back office context (/admin), at which a stored file is served. */
    public static final String FILE_PATH_PREFIX = "/files/";

    private final FileUploadRepository fileUploadRepository;

    public FileUploadService(FileUploadRepository fileUploadRepository) {
        this.fileUploadRepository = fileUploadRepository;
    }

    public FileUploadResponse uploadFile(MultipartFile file, FileType fileType, int userId, String description) {
        return store(file, fileType, userId, null, description);
    }

    public FileUploadResponse uploadFileByUuid(MultipartFile file, FileType fileType, String userUuid, String description) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new IllegalArgumentException("User UUID cannot be null or empty");
        }
        return store(file, fileType, null, userUuid, description);
    }

    private FileUploadResponse store(MultipartFile file, FileType fileType, Integer userId, String userUuid, String description) {
        FileUploadResponse result = new FileUploadResponse();
        byte[] data;
        try {
            data = file == null ? null : file.getBytes();
        } catch (IOException e) {
            log.error("Could not read uploaded file: {}", e.getMessage());
            result.setStatus(false);
            result.setMessage("The file could not be read. Please try again.");
            return result;
        }
        String problem = StoredFiles.rejectionReason(data, false);
        if (problem != null) {
            result.setStatus(false);
            result.setMessage(problem);
            return result;
        }

        String contentType = StoredFiles.detectContentType(data);
        FileUpload stored = new FileUpload();
        stored.setFileName(StoredFiles.safeFileName(file.getOriginalFilename(), contentType));
        stored.setFileType(fileType);
        stored.setFilePath("database");
        stored.setUserId(userId);
        stored.setUserUuid(userUuid);
        stored.setDescription(description);
        stored.setCreatedAt(LocalDateTime.now());
        stored.setFileSize((long) data.length);
        stored.setContentType(contentType);
        stored.setContent(data);
        stored.setSha256(StoredFiles.sha256Hex(data));
        stored = fileUploadRepository.save(stored);

        result.setStatus(true);
        result.setFileId(Math.toIntExact(stored.getId()));
        result.setFilePath(FILE_PATH_PREFIX + stored.getId());
        result.setMessage("File stored.");
        return result;
    }

    /** A stored file that has content, for the authenticated file endpoint. */
    public Optional<FileUpload> findStoredFile(long fileId) {
        return fileUploadRepository.findById(fileId).filter(f -> f.getContent() != null);
    }

    public FileAccessResponse getFileAccessDetails(int fileId) {
        FileAccessResponse response = new FileAccessResponse();
        Optional<FileUpload> file = findStoredFile(fileId);
        response.setStatus(file.isPresent());
        file.ifPresent(f -> {
            response.setFileName(f.getFileName());
            response.setFileUrl(FILE_PATH_PREFIX + f.getId());
        });
        return response;
    }

    /** Like {@link #getFileAccessDetails} but never fails: no URL when there is no stored file. */
    public FileAccessResponse getFileAccessDetailsWithFallback(int fileId, FileType fileType) {
        return getFileAccessDetails(fileId);
    }
}
