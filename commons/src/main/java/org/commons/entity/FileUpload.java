package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.commons.enums.FileType;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_uploads")
public class FileUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50, nullable = false)
    private FileType fileType;
    
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "user_uuid", length = 36)
    private String userUuid;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "log_message")
    private String logMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "remote_file_id")
    private Integer remoteFileId;

    // The file itself, kept in the database (app schema: no API-role grants) so
    // uploads survive container restarts and are only ever served through an
    // authenticated portal endpoint. Never logged or compared.
    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "content")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private byte[] content;

    @Column(name = "sha256", length = 64)
    private String sha256;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
