package org.paykesho.notificationservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

/** Requires the internal service token for notification API routes. */
@Component
public class InternalNotificationTokenFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-PayKesho-Internal-Token";
    private final String configuredToken;

    public InternalNotificationTokenFilter(@Value("${PAYKESHO_INTERNAL_TOKEN:}") String configuredToken) {
        this.configuredToken = configuredToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getServletPath().startsWith("/api/notifications/")) {
            chain.doFilter(request, response);
            return;
        }
        String supplied = request.getHeader(HEADER);
        if (!constantTimeMatch(configuredToken, supplied)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Unauthorized notification-service request.\"}");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("paykesho-internal", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
        chain.doFilter(request, response);
    }

    private static boolean constantTimeMatch(String expected, String supplied) {
        if (expected == null || expected.isEmpty() || supplied == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
