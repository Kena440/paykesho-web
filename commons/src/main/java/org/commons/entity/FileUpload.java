package org.commons.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.enums.FileType;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "file_uploads")
public class FileUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "file_name", length = 128, nullable = false)
    private String fileName;
    @Column(name = "file_path")
    private String filePath;
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50)
    private FileType fileType;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    @Column(name = "created_at", length = 128)
    private Date createdAt;
}
