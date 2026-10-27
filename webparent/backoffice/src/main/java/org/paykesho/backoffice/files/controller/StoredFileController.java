package org.paykesho.backoffice.files.controller;

import org.commons.entity.FileUpload;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<byte[]> open(@PathVariable("id") long id) {
        return fileUploadService.findStoredFile(id)
                .map(StoredFileController::inline)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
