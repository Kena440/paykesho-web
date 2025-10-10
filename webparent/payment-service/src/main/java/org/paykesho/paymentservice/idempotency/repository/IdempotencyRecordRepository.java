package org.paykesho.paymentservice.idempotency.repository;

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
 * Repository for managing idempotency records
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Integer> {

    /**
     * Find an idempotency record by its key
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all expired records for cleanup
     */
    @Query("SELECT i FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    List<IdempotencyRecord> findExpiredRecords(@Param("now") Date now);

    /**
     * Delete all expired records (for cleanup job)
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") Date now);

    /**
     * Find records by endpoint and date range
     */
    @Query("SELECT i FROM IdempotencyRecord i WHERE i.endpoint = :endpoint AND i.createdAt BETWEEN :startDate AND :endDate")
    List<IdempotencyRecord> findByEndpointAndDateRange(
            @Param("endpoint") String endpoint,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    /**
     * Find processing records older than specified time (potential stuck operations)
     */
    @Query("SELECT i FROM IdempotencyRecord i WHERE i.status = 'PROCESSING' AND i.createdAt < :threshold")
    List<IdempotencyRecord> findStuckProcessingRecords(@Param("threshold") Date threshold);

    /**
     * Count idempotency records by user in a time window
     */
    @Query("SELECT COUNT(i) FROM IdempotencyRecord i WHERE i.userIdentifier = :userId AND i.createdAt > :since")
    long countByUserSince(@Param("userId") String userId, @Param("since") Date since);

    /**
     * Check if idempotency key exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find by idempotency key and status
     */
    Optional<IdempotencyRecord> findByIdempotencyKeyAndStatus(String idempotencyKey, String status);
}

