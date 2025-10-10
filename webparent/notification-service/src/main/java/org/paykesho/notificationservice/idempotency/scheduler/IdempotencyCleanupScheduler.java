package org.paykesho.notificationservice.idempotency.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.paykesho.notificationservice.idempotency.service.IdempotencyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private final IdempotencyService idempotencyService;

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
}

