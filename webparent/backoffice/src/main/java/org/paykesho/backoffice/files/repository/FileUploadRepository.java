package org.paykesho.backoffice.files.repository;

import org.commons.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    // Additional query methods can be defined here
}
