package org.paykesho.notificationservice.idempotency.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.IdempotencyRecord;
import org.paykesho.notificationservice.idempotency.service.IdempotencyService;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY_ATTR = "idempotency.key";
    
    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        if (!isStateChangingMethod(method) || shouldSkipIdempotency(request)) return true;

        String idempotencyKey = getIdempotencyKey(request);
        Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
        
        if (existingRecord.isPresent() && existingRecord.get().isCompleted()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return replayResponse(response, existingRecord.get());
        }

        try {
            idempotencyService.createIdempotencyRecord(
                idempotencyKey, request.getRequestURI(), request.getMethod(),
                getRequestBody(request), getUserIdentifier(request)
            );
            request.setAttribute(IDEMPOTENCY_KEY_ATTR, idempotencyKey);
        } catch (Exception e) {
            log.error("Error creating idempotency record: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String idempotencyKey = (String) request.getAttribute(IDEMPOTENCY_KEY_ATTR);
        if (idempotencyKey == null) return;

        try {
            if (ex != null) {
                idempotencyService.markAsFailed(idempotencyKey, ex.getMessage(), response.getStatus());
            } else if (response.getStatus() >= 200 && response.getStatus() < 300) {
                idempotencyService.markAsCompleted(idempotencyKey, response.getStatus(), getResponseBody(response), null);
            } else if (response.getStatus() >= 400) {
                idempotencyService.markAsFailed(idempotencyKey, "HTTP " + response.getStatus(), response.getStatus());
            }
        } catch (Exception e) {
            log.error("Error updating idempotency record: {}", e.getMessage());
        }
    }

    private boolean isStateChangingMethod(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || 
               method == HttpMethod.PATCH || method == HttpMethod.DELETE;
    }

    private boolean shouldSkipIdempotency(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/actuator/") || uri.contains("/health") || 
               uri.contains("/login") || uri.contains("/logout") || uri.contains("/auth") ||
               uri.contains("/forgot-password") || uri.contains("/reset-password") ||
               uri.contains("/assets/") || uri.contains("/images/") || uri.contains("/css/") || uri.contains("/js/");
    }

    private String getIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        return (key != null && !key.trim().isEmpty()) ? key : generateIdempotencyKey(request);
    }

    private String generateIdempotencyKey(HttpServletRequest request) {
        try {
            String keySource = request.getMethod() + ":" + request.getRequestURI() + ":" + 
                             getUserIdentifier(request) + ":" + getRequestBody(request);
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
            return "AUTO_" + UUID.randomUUID().toString();
        }
    }

    private boolean replayResponse(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus() != null ? record.getResponseStatus() : 200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Idempotency-Replayed", "true");
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
        return false;
    }

    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            byte[] content = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
            return content.length > 0 ? new String(content, StandardCharsets.UTF_8) : "";
        }
        return "";
    }

    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            byte[] content = ((ContentCachingResponseWrapper) response).getContentAsByteArray();
            return content.length > 0 ? new String(content, StandardCharsets.UTF_8) : "";
        }
        return "";
    }

    private String getUserIdentifier(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return request.getRemoteAddr();
    }
}

