package org.paykesho.backoffice.files.controller;

import org.commons.entity.FileUpload;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Opens stored files (client payslips, logos, photos) for signed-in back-office
 * staff. The back office requires authentication for every path except its
 * sign-in pages, so anonymous visitors are sent to sign in.
 */
@RestController
public class StoredFileController {

    private final FileUploadService fileUploadService;

    public StoredFileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> open(@PathVariable("id") long id, Authentication authentication) {
        return fileUploadService.findStoredFile(id)
                .map(file -> {
                    boolean reviewer = authentication != null && authentication.getAuthorities().stream()
                            .anyMatch(a -> "Admin".equals(a.getAuthority()) || "SuperAdmin".equals(a.getAuthority()));
                    boolean sensitive = file.getFileType() == org.commons.enums.FileType.nrc
                            || file.getFileType() == org.commons.enums.FileType.selfie
                            || file.getFileType() == org.commons.enums.FileType.payslip;
                    // PII documents are restricted to the same roles that can review KYC.
                    if (sensitive && !reviewer) return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
                    return inline(file);
                })
                .orElseGet(() -> ResponseEntity.<byte[]>notFound().build());
    }

    private static ResponseEntity<byte[]> inline(FileUpload f) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(f.getContent());
    }
}
