package org.paykesho.fileservice.service;

import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.commons.test.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.fileservice.config.FileStorageProperties;
import org.paykesho.fileservice.repository.FileUploadRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Unit Tests")
class FileStorageServiceTest extends BaseUnitTest {

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private FileUploadRepository fileUploadRepository;

    @Mock
    private MultipartFile multipartFile;

    private FileStorageService fileStorageService;

    private String testBasePath;
    private String testBaseUrl;

    @BeforeEach
    void setUp() {
        testBasePath = "/tmp/uploads";
        testBaseUrl = "http://localhost:8080";
        lenient().when(fileStorageProperties.getBaseUrl()).thenReturn(testBaseUrl);
        lenient().when(fileStorageProperties.getFullPath(anyString())).thenReturn(testBasePath + "/logos");
        // Manually instantiate service after mocks are set up
        fileStorageService = new FileStorageService(fileStorageProperties, fileUploadRepository);
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void testStoreFile_EmptyFile() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, FileType.logo, 1, "Test description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot upload empty file");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when file size exceeds maximum")
    void testStoreFile_FileSizeExceedsMaximum() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(10_000_000L); // 10MB
        when(fileStorageProperties.getMaxFileSize()).thenReturn(5_000_000L); // 5MB max

        // When/Then
        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, FileType.logo, 1, "Test description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when content type is invalid")
    void testStoreFile_InvalidContentType() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("application/x-executable");
        when(fileStorageProperties.getMaxFileSize()).thenReturn(5_000_000L);

        // When/Then
        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, FileType.logo, 1, "Test description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file type");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when filename contains path traversal")
    void testStoreFile_PathTraversal() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getOriginalFilename()).thenReturn("../../../etc/passwd");
        when(fileStorageProperties.getMaxFileSize()).thenReturn(5_000_000L);

        // When/Then
        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, FileType.logo, 1, "Test description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid path sequence");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when filename is null")
    void testStoreFile_NullFilename() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(fileStorageProperties.getMaxFileSize()).thenReturn(5_000_000L);

        // When/Then
        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, FileType.logo, 1, "Test description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename cannot be null");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get file metadata successfully")
    void testGetFileMetadata_Success() {
        // Given
        Long fileId = 1L;
        FileUpload fileUpload = new FileUpload();
        fileUpload.setId(fileId);
        fileUpload.setFileName("test.png");
        fileUpload.setFilePath("/tmp/uploads/logos/test.png");

        when(fileUploadRepository.findById(fileId)).thenReturn(Optional.of(fileUpload));

        // When
        Optional<FileUpload> result = fileStorageService.getFileMetadata(fileId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(fileId);
        assertThat(result.get().getFileName()).isEqualTo("test.png");
        verify(fileUploadRepository, times(1)).findById(fileId);
    }

    @Test
    @DisplayName("Should return empty when file not found")
    void testGetFileMetadata_NotFound() {
        // Given
        Long fileId = 999L;
        when(fileUploadRepository.findById(fileId)).thenReturn(Optional.empty());

        // When
        Optional<FileUpload> result = fileStorageService.getFileMetadata(fileId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when file ID is null")
    void testGetFileMetadata_NullId() {
        // When
        Optional<FileUpload> result = fileStorageService.getFileMetadata(null);

        // Then
        assertThat(result).isEmpty();
        verify(fileUploadRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when user UUID is null for storeFileByUuid")
    void testStoreFileByUuid_NullUuid() {
        // When/Then
        assertThatThrownBy(() -> fileStorageService.storeFileByUuid(multipartFile, FileType.logo, null, "Test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User UUID cannot be null");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate file URL correctly")
    void testGenerateFileUrl() {
        // Given
        FileUpload fileUpload = new FileUpload();
        fileUpload.setId(1L);
        fileUpload.setFileType(FileType.logo);
        fileUpload.setFilePath("/tmp/uploads/logos/uuid_filename.png");

        // When
        String url = fileStorageService.generateFileUrl(fileUpload);

        // Then
        assertThat(url).isNotNull();
        assertThat(url).contains(testBaseUrl);
        assertThat(url).contains("uploads");
    }
}
