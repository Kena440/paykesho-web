package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity for tracking idempotent requests to prevent duplicate operations.
 * Stores idempotency keys with their corresponding responses for replay.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_expires_at", columnList = "expires_at"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class IdempotencyRecord extends IdBasedEntity {

    /**
     * Unique idempotency key provided by client or generated
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    /**
     * The endpoint/operation being performed
     */
    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    /**
     * HTTP method (POST, PUT, PATCH, DELETE)
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /**
     * Hash of the request body to detect duplicate requests with same key but different payload
     */
    @Column(name = "request_hash", length = 64)
    private String requestHash;

    /**
     * Serialized request body for logging/debugging
     */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    /**
     * HTTP status code of the response
     */
    @Column(name = "response_status")
    private Integer responseStatus;

    /**
     * Serialized response body to replay for duplicate requests
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /**
     * Response headers (JSON format)
     */
    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    /**
     * When the record was created
     */
    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * When the record expires and can be cleaned up
     * Default: 24-48 hours from creation
     */
    @Column(name = "expires_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    /**
     * Processing status: PROCESSING, COMPLETED, FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PROCESSING";

    /**
     * User/client identifier who made the request
     */
    @Column(name = "user_identifier", length = 255)
    private String userIdentifier;

    /**
     * Additional metadata (JSON format)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Error message if operation failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of times this key was attempted
     */
    @Column(name = "attempt_count")
    private Integer attemptCount = 1;

    /**
     * Last attempt timestamp
     */
    @Column(name = "last_attempt_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastAttemptAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (lastAttemptAt == null) {
            lastAttemptAt = new Date();
        }
        if (expiresAt == null) {
            // Default expiration: 24 hours
            expiresAt = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastAttemptAt = new Date();
    }

    /**
     * Check if this record has expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Date());
    }

    /**
     * Check if this record is still processing
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(status);
    }

    /**
     * Check if this record represents a completed operation
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * Check if this record represents a failed operation
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}

