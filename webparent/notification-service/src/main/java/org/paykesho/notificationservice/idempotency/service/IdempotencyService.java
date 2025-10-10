package org.paykesho.notificationservice.idempotency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.IdempotencyRecord;
import org.paykesho.notificationservice.idempotency.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing idempotency operations in notification service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> checkIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        return idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord createIdempotencyRecord(
            String idempotencyKey,
            String endpoint,
            String httpMethod,
            String requestBody,
            String userIdentifier) {
        
        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.warn("Idempotency key already exists: {}", idempotencyKey);
            IdempotencyRecord record = existing.get();
            record.setAttemptCount(record.getAttemptCount() + 1);
            record.setLastAttemptAt(new Date());
            return idempotencyRecordRepository.save(record);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setEndpoint(endpoint);
        record.setHttpMethod(httpMethod);
        record.setRequestBody(requestBody);
        record.setRequestHash(computeHash(requestBody));
        record.setUserIdentifier(userIdentifier);
        record.setStatus("PROCESSING");
        record.setCreatedAt(new Date());
        record.setLastAttemptAt(new Date());
        record.setExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));

        return idempotencyRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCompleted(String idempotencyKey, Integer responseStatus, String responseBody, String responseHeaders) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (recordOpt.isEmpty()) {
            log.error("Idempotency record not found for key: {}", idempotencyKey);
            return;
        }

        IdempotencyRecord record = recordOpt.get();
        record.setStatus("COMPLETED");
        record.setResponseStatus(responseStatus);
        record.setResponseBody(responseBody);
        record.setResponseHeaders(responseHeaders);
        
        idempotencyRecordRepository.save(record);
        log.info("Marked idempotency record as completed: {}", idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(String idempotencyKey, String errorMessage, Integer responseStatus) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (recordOpt.isEmpty()) {
            log.error("Idempotency record not found for key: {}", idempotencyKey);
            return;
        }

        IdempotencyRecord record = recordOpt.get();
        record.setStatus("FAILED");
        record.setErrorMessage(errorMessage);
        record.setResponseStatus(responseStatus);
        
        idempotencyRecordRepository.save(record);
        log.info("Marked idempotency record as failed: {}", idempotencyKey);
    }

    @Transactional
    public int cleanupExpiredRecords() {
        Date now = new Date();
        int deletedCount = idempotencyRecordRepository.deleteExpiredRecords(now);
        log.info("Cleaned up {} expired idempotency records", deletedCount);
        return deletedCount;
    }

    @Transactional
    public int cleanupStuckRecords() {
        Date threshold = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        var stuckRecords = idempotencyRecordRepository.findStuckProcessingRecords(threshold);
        
        for (IdempotencyRecord record : stuckRecords) {
            record.setStatus("FAILED");
            record.setErrorMessage("Operation timeout - stuck in processing state");
            idempotencyRecordRepository.save(record);
        }
        
        log.info("Cleaned up {} stuck processing records", stuckRecords.size());
        return stuckRecords.size();
    }

    private String computeHash(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error computing hash: {}", e.getMessage());
            return "";
        }
    }

    public boolean validateRequestHash(IdempotencyRecord record, String currentRequestBody) {
        if (record.getRequestHash() == null || record.getRequestHash().isEmpty()) {
            return true;
        }
        
        String currentHash = computeHash(currentRequestBody);
        boolean matches = record.getRequestHash().equals(currentHash);
        
        if (!matches) {
            log.warn("Request hash mismatch for idempotency key: {}. Possible replay attack.", record.getIdempotencyKey());
        }
        
        return matches;
    }

    public boolean isSafeToRetry(IdempotencyRecord record) {
        if (record.isCompleted()) {
            return false;
        }
        
        if (record.getAttemptCount() != null && record.getAttemptCount() > 3) {
            return false;
        }
        
        if (record.isProcessing()) {
            long processingTime = System.currentTimeMillis() - record.getCreatedAt().getTime();
            if (processingTime < 5 * 60 * 1000) {
                return false;
            }
        }
        
        return true;
    }
}

