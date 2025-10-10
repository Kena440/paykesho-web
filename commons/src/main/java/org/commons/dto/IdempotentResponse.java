package org.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for idempotent responses
 * Includes metadata about whether this is a cached/replayed response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentResponse<T> {
    
    /**
     * The actual response data
     */
    private T data;
    
    /**
     * The idempotency key used
     */
    private String idempotencyKey;
    
    /**
     * Whether this response was replayed from cache (true) or newly created (false)
     */
    private boolean replayed;
    
    /**
     * Timestamp when the original request was processed
     */
    private Long originalRequestTime;
    
    /**
     * HTTP status code
     */
    private Integer statusCode;
    
    /**
     * Optional message
     */
    private String message;

    public static <T> IdempotentResponse<T> newResponse(T data, String idempotencyKey, Integer statusCode) {
        IdempotentResponse<T> response = new IdempotentResponse<>();
        response.setData(data);
        response.setIdempotencyKey(idempotencyKey);
        response.setReplayed(false);
        response.setOriginalRequestTime(System.currentTimeMillis());
        response.setStatusCode(statusCode);
        return response;
    }

    public static <T> IdempotentResponse<T> replayedResponse(T data, String idempotencyKey, Integer statusCode, Long originalTime) {
        IdempotentResponse<T> response = new IdempotentResponse<>();
        response.setData(data);
        response.setIdempotencyKey(idempotencyKey);
        response.setReplayed(true);
        response.setOriginalRequestTime(originalTime);
        response.setStatusCode(statusCode);
        response.setMessage("This request was already processed. Returning cached response.");
        return response;
    }
}

