package org.paykesho.backoffice.user.repository;

import org.commons.entity.DeviceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceMetadataRepository extends JpaRepository<DeviceMetadata, Integer> {
    List<DeviceMetadata> findByUserId(Integer userId);
}
