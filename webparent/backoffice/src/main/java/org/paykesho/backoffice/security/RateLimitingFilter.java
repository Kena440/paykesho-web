package org.paykesho.backoffice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting filter to prevent abuse
 */
@Slf4j
@Component
public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 120; // Increased for normal usage
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 15; // More reasonable for login attempts
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute
    private final ConcurrentHashMap<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();
    @Value("${paykesho.security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIpAddress(httpRequest);
        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Skip rate limiting if disabled or for static resources
        if (!rateLimitingEnabled || isStaticResource(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Apply different limits for different endpoints
        int maxRequests;
        String rateLimitKey;

        if (isLoginEndpoint(requestPath) && "POST".equals(method)) {
            // Only rate limit actual login POST attempts, not GET requests to login page
            maxRequests = MAX_LOGIN_ATTEMPTS_PER_MINUTE;
            rateLimitKey = clientIp + ":login";
        } else {
            maxRequests = MAX_REQUESTS_PER_MINUTE;
            rateLimitKey = clientIp + ":general";
        }

        if (isRateLimited(rateLimitKey, maxRequests)) {
            log.warn("Rate limit exceeded for IP: {} on path: {} (method: {}) - Key: {}, Max: {}",
                    clientIp, requestPath, method, rateLimitKey, maxRequests);
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        // Debug logging for login attempts
        if (isLoginEndpoint(requestPath)) {
            RequestWindow window = requestCounts.get(rateLimitKey);
            int currentCount = window != null ? window.requestCount.get() : 0;
            log.debug("Login attempt from IP: {} - Count: {}/{} on path: {}",
                    clientIp, currentCount, maxRequests, requestPath);
        }

        chain.doFilter(request, response);
    }

    private boolean isLoginEndpoint(String path) {
        return path != null && (path.equals("/login") || path.contains("/reset-password") || path.contains("/forgot-password"));
    }

    private boolean isStaticResource(String path) {
        if (path == null) return false;

        return path.startsWith("/assets/") ||
                path.startsWith("/images/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/fonts/") ||
                path.startsWith("/static/") ||
                path.equals("/favicon.ico") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".gif") ||
                path.endsWith(".ico") ||
                path.endsWith(".svg") ||
                path.endsWith(".woff") ||
                path.endsWith(".woff2") ||
                path.endsWith(".ttf") ||
                path.endsWith(".eot") ||
                path.endsWith(".webmanifest") ||
                path.endsWith(".map");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private boolean isRateLimited(String rateLimitKey, int maxRequests) {
        long currentTime = System.currentTimeMillis();

        RequestWindow window = requestCounts.computeIfAbsent(rateLimitKey, k -> new RequestWindow(currentTime));

        // Reset window if expired
        if (currentTime - window.windowStart > WINDOW_SIZE_MS) {
            window.reset(currentTime);
        }

        // Increment request count
        int currentCount = window.requestCount.incrementAndGet();

        // Clean up old entries periodically
        if (currentTime % 60000 == 0) { // Every minute
            cleanupOldEntries(currentTime);
        }

        return currentCount > maxRequests;
    }

    private void cleanupOldEntries(long currentTime) {
        requestCounts.entrySet().removeIf(entry ->
                currentTime - entry.getValue().windowStart > WINDOW_SIZE_MS * 2);
    }

    private static class RequestWindow {
        private final AtomicInteger requestCount;
        private volatile long windowStart;

        public RequestWindow(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(0);
        }

        public void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.requestCount.set(0);
        }
    }
}
