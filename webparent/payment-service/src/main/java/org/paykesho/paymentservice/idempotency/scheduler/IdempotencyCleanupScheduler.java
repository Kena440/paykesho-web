package org.paykesho.paymentservice.idempotency.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.paykesho.paymentservice.idempotency.service.IdempotencyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to cleanup expired and stuck idempotency records
 * Runs daily at 2 AM to maintain database hygiene
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private final IdempotencyService idempotencyService;

    /**
     * Clean up expired idempotency records
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredRecords() {
        log.info("Starting cleanup of expired idempotency records");
        try {
            int deletedCount = idempotencyService.cleanupExpiredRecords();
            log.info("Successfully cleaned up {} expired idempotency records", deletedCount);
        } catch (Exception e) {
            log.error("Error cleaning up expired idempotency records: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up stuck processing records
     * Runs daily at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void cleanupStuckRecords() {
        log.info("Starting cleanup of stuck processing records");
        try {
            int cleanedCount = idempotencyService.cleanupStuckRecords();
            log.info("Successfully cleaned up {} stuck processing records", cleanedCount);
        } catch (Exception e) {
            log.error("Error cleaning up stuck processing records: {}", e.getMessage(), e);
        }
    }

    /**
     * Log idempotency statistics
     * Runs hourly
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logIdempotencyStatistics() {
        log.info("Idempotency cleanup scheduler is active and running");
    }
}

