package org.paykesho.backoffice.security.repository;

import org.commons.entity.SecurityAudit;
import org.commons.enums.SecurityAuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Integer> {
    
    List<SecurityAudit> findByStatus(SecurityAuditStatus status);
    
    List<SecurityAudit> findByTableNameAndRecordId(String tableName, Integer recordId);
    
    @Query("SELECT s FROM SecurityAudit s WHERE s.status = 'PENDING' ORDER BY s.severity DESC, s.scanDate DESC")
    List<SecurityAudit> findPendingOrderBySeverity();
    
    @Query("SELECT s FROM SecurityAudit s WHERE s.scanDate >= ?1 ORDER BY s.scanDate DESC")
    List<SecurityAudit> findRecentScans(Date sinceDate);
    
    @Query("SELECT COUNT(s) FROM SecurityAudit s WHERE s.status = 'PENDING'")
    Long countPendingAudits();
    
    @Query("SELECT s.tableName, COUNT(s) FROM SecurityAudit s WHERE s.status = 'PENDING' GROUP BY s.tableName")
    List<Object[]> countPendingByTable();
}

