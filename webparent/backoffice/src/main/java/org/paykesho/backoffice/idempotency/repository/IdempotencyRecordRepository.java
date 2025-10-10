package org.paykesho.backoffice.idempotency.repository;

import org.commons.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing idempotency records in backoffice
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Integer> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT i FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    List<IdempotencyRecord> findExpiredRecords(@Param("now") Date now);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") Date now);

    @Query("SELECT i FROM IdempotencyRecord i WHERE i.status = 'PROCESSING' AND i.createdAt < :threshold")
    List<IdempotencyRecord> findStuckProcessingRecords(@Param("threshold") Date threshold);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<IdempotencyRecord> findByIdempotencyKeyAndStatus(String idempotencyKey, String status);
}

