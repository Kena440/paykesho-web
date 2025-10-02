package org.paykesho.backoffice.notification.repository;

import org.commons.entity.NotificationRequest;
import org.commons.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for NotificationRequest entity in backoffice module
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    /**
     * Count notifications by source module
     */
    long countBySourceModule(String sourceModule);

    /**
     * Count notifications by source module and status
     */
    long countBySourceModuleAndStatus(String sourceModule, NotificationStatus status);
}
