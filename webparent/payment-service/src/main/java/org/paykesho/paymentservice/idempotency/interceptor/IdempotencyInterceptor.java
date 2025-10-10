package org.paykesho.paymentservice.idempotency.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.annotation.Idempotent;
import org.commons.entity.IdempotencyRecord;
import org.paykesho.paymentservice.idempotency.service.IdempotencyService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Global interceptor for handling idempotency across all REST endpoints
 * Automatically applies idempotency to POST, PUT, PATCH, DELETE methods
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_PROCESSED_ATTR = "idempotency.processed";
    private static final String IDEMPOTENCY_KEY_ATTR = "idempotency.key";
    
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only process for REST controllers
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        
        // Only apply idempotency to state-changing methods
        if (!isStateChangingMethod(method)) {
            return true;
        }

        // Check if method explicitly uses @Idempotent annotation
        Idempotent idempotentAnnotation = handlerMethod.getMethodAnnotation(Idempotent.class);
        
        // Skip if explicitly disabled or for specific paths
        if (shouldSkipIdempotency(request, idempotentAnnotation)) {
            return true;
        }

        // Get or generate idempotency key
        String idempotencyKey = getIdempotencyKey(request, idempotentAnnotation);
        
        // Check if this request was already processed
        Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
        
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            
            if (record.isCompleted()) {
                // Return cached response
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return replayResponse(response, record);
            }
            
            if (record.isProcessing()) {
                // Check if it's been processing too long
                long processingTime = System.currentTimeMillis() - record.getCreatedAt().getTime();
                if (processingTime < 5 * 60 * 1000) { // 5 minutes
                    // Still processing, return 409 Conflict
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Request is currently being processed\",\"retryAfter\":\"30\"}");
                    return false;
                }
            }
        }

        // Create idempotency record for new request
        try {
            String requestBody = getRequestBody(request);
            idempotencyService.createIdempotencyRecord(
                idempotencyKey,
                request.getRequestURI(),
                request.getMethod(),
                requestBody,
                getUserIdentifier(request)
            );
            
            // Store key for afterCompletion
            request.setAttribute(IDEMPOTENCY_KEY_ATTR, idempotencyKey);
            
        } catch (Exception e) {
            log.error("Error creating idempotency record: {}", e.getMessage());
            // Continue processing even if idempotency fails
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String idempotencyKey = (String) request.getAttribute(IDEMPOTENCY_KEY_ATTR);
        
        if (idempotencyKey == null) {
            return; // No idempotency was applied
        }

        try {
            if (ex != null) {
                // Request failed with exception
                idempotencyService.markAsFailed(idempotencyKey, ex.getMessage(), response.getStatus());
            } else if (response.getStatus() >= 200 && response.getStatus() < 300) {
                // Success - store response for replay
                String responseBody = getResponseBody(response);
                idempotencyService.markAsCompleted(
                    idempotencyKey,
                    response.getStatus(),
                    responseBody,
                    getResponseHeaders(response)
                );
            } else if (response.getStatus() >= 400) {
                // Client or server error
                idempotencyService.markAsFailed(
                    idempotencyKey,
                    "HTTP " + response.getStatus(),
                    response.getStatus()
                );
            }
        } catch (Exception e) {
            log.error("Error updating idempotency record: {}", e.getMessage());
        }
    }

    /**
     * Check if this HTTP method should have idempotency applied
     */
    private boolean isStateChangingMethod(HttpMethod method) {
        return method == HttpMethod.POST || 
               method == HttpMethod.PUT || 
               method == HttpMethod.PATCH || 
               method == HttpMethod.DELETE;
    }

    /**
     * Check if idempotency should be skipped for this request
     */
    private boolean shouldSkipIdempotency(HttpServletRequest request, Idempotent annotation) {
        String uri = request.getRequestURI();
        
        // Skip for health checks, metrics, etc.
        if (uri.contains("/actuator/") || 
            uri.contains("/health") || 
            uri.contains("/metrics")) {
            return true;
        }
        
        // Skip for login/logout
        if (uri.contains("/login") || uri.contains("/logout") || uri.contains("/auth")) {
            return true;
        }
        
        // Skip for password reset and static resources
        if (uri.contains("/forgot-password") || uri.contains("/reset-password") ||
            uri.contains("/assets/") || uri.contains("/images/") || uri.contains("/css/") || uri.contains("/js/")) {
            return true;
        }
        
        return false;
    }

    /**
     * Get idempotency key from header or generate one
     */
    private String getIdempotencyKey(HttpServletRequest request, Idempotent annotation) {
        // First check for explicit header
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        if (key != null && !key.trim().isEmpty()) {
            return key;
        }
        
        // Check if annotation requires key
        if (annotation != null && annotation.required()) {
            // Key is required but not provided - use request hash
            log.warn("Idempotency key required but not provided. Generating from request.");
        }
        
        // Auto-generate based on request characteristics
        return generateIdempotencyKey(request);
    }

    /**
     * Generate idempotency key from request characteristics
     */
    private String generateIdempotencyKey(HttpServletRequest request) {
        try {
            String requestBody = getRequestBody(request);
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String userIdentifier = getUserIdentifier(request);
            
            String keySource = method + ":" + uri + ":" + userIdentifier + ":" + requestBody;
            
            // Create hash
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keySource.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return "AUTO_" + hexString.toString().substring(0, 32);
        } catch (Exception e) {
            log.error("Error generating idempotency key: {}", e.getMessage());
            return "AUTO_" + UUID.randomUUID().toString();
        }
    }

    /**
     * Replay cached response to client
     */
    private boolean replayResponse(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus() != null ? record.getResponseStatus() : 200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Idempotency-Replayed", "true");
        
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        } else {
            response.getWriter().write("{\"message\":\"Request already processed\"}");
        }
        
        return false; // Don't continue with handler
    }

    /**
     * Extract request body from request
     */
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    /**
     * Extract response body from response
     */
    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    /**
     * Extract response headers as JSON string
     */
    private String getResponseHeaders(HttpServletResponse response) {
        try {
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            for (String headerName : response.getHeaderNames()) {
                headers.put(headerName, response.getHeader(headerName));
            }
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Get user identifier from request
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // Try to get from principal
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        
        // Try to get from session
        if (request.getSession(false) != null) {
            Object user = request.getSession().getAttribute("user");
            if (user != null) {
                return user.toString();
            }
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
}

