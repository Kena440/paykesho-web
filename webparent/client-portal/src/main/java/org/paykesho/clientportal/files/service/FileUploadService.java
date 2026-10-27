package org.paykesho.clientportal.files.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.commons.utils.StoredFiles;
import org.paykesho.clientportal.files.dto.FileAccessResponse;
import org.paykesho.clientportal.files.dto.FileUploadResponse;
import org.paykesho.clientportal.files.repository.FileUploadRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Stores files clients upload (payslips) in the database, owned by the client
 * who uploaded them. Files are served only through the authenticated
 * {@code /files/{id}} endpoints (here for the owner, and in the back office
 * for staff); there is no public path to them.
 *
 * Previously uploads went to the separate file service, which is not deployed
 * and kept files on a container disk that is wiped on restart.
 */
@Slf4j
@Service
@Transactional
public class FileUploadService {

    /** Path, relative to a portal's context, at which a stored file is served. */
    public static final String FILE_PATH_PREFIX = "/files/";

    private final FileUploadRepository fileUploadRepository;

    public FileUploadService(FileUploadRepository fileUploadRepository) {
        this.fileUploadRepository = fileUploadRepository;
    }

    /**
     * Stores a client's file. Returns status=false with a message the client can
     * act on (wrong type, too large, empty) instead of throwing.
     */
    public FileUploadResponse uploadOwnedFile(MultipartFile file, FileType fileType, String ownerUuid, String description) {
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
        stored.setUserUuid(ownerUuid);
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

    /** Kept for existing callers; prefer {@link #uploadOwnedFile} so the file has an owner. */
    public FileUploadResponse uploadFile(MultipartFile file, FileType fileType, int userId, String description) {
        return uploadOwnedFile(file, fileType, null, description);
    }

    /** The stored file, if it exists and belongs to the given client. */
    public Optional<FileUpload> findOwnedFile(long fileId, String ownerUuid) {
        if (ownerUuid == null) return Optional.empty();
        return fileUploadRepository.findById(fileId)
                .filter(f -> f.getContent() != null && ownerUuid.equals(f.getUserUuid()));
    }

    public FileAccessResponse getFileAccessDetails(int fileId) {
        FileAccessResponse response = new FileAccessResponse();
        Optional<FileUpload> file = fileUploadRepository.findById((long) fileId).filter(f -> f.getContent() != null);
        response.setStatus(file.isPresent());
        file.ifPresent(f -> {
            response.setFileName(f.getFileName());
            response.setFileUrl(FILE_PATH_PREFIX + f.getId());
        });
        return response;
    }
}
