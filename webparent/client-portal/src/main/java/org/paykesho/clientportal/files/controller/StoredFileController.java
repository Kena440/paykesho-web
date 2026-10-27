package org.paykesho.clientportal.files.controller;

import org.commons.entity.FileUpload;
import org.paykesho.clientportal.files.service.FileUploadService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Lets a signed-in client open the files they uploaded, and nobody else's. */
@RestController
public class StoredFileController {

    private final FileUploadService fileUploadService;

    public StoredFileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> open(@PathVariable("id") long id, @AuthenticationPrincipal ClientUserDetails user) {
        String owner = user == null || user.getClient() == null ? null : user.getClient().getUuid();
        Optional<FileUpload> file = fileUploadService.findOwnedFile(id, owner);
        // Same answer for "missing" and "someone else's", so file ids reveal nothing.
        return file.map(StoredFileController::inline).orElseGet(() -> ResponseEntity.notFound().build());
    }

    static ResponseEntity<byte[]> inline(FileUpload f) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(f.getContent());
    }
}
