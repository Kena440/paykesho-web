package org.paykesho.backoffice.files.controller;

import org.paykesho.backoffice.files.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads/rest")
public class UploadsRestController {

    private final FileUploadService fileUploadService;

    @Autowired
    public UploadsRestController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

/*    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("fileType") FileType fileType, @RequestParam("userId") int userId, @RequestParam("description") String description) {

        boolean success = fileUploadService.uploadFile(file, fileType, userId, description);

        if (success) {
            return ResponseEntity.ok("File uploaded successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
        }
    }*/

/*    @GetMapping("/access/{fileId}")
    public ResponseEntity<String> getAccessDetails(@PathVariable int fileId) {
        String accessDetails = fileUploadService.getFileAccessDetails(fileId);
        if (accessDetails != null) {
            return ResponseEntity.ok(accessDetails);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Access details not found for file ID: " + fileId);
        }
    }*/
}
