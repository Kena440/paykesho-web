package org.paykesho.fileservice.repository;

import org.commons.entity.FileUpload;
import org.commons.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    
    List<FileUpload> findByUserId(Integer userId);
    
    List<FileUpload> findByFileType(FileType fileType);
}

