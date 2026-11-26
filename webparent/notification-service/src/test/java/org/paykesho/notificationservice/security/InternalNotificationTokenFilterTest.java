package org.paykesho.notificationservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class InternalNotificationTokenFilterTest {
    private InternalNotificationTokenFilter filter() {
        return new InternalNotificationTokenFilter("secret-token");
    }

    @Test
    void rejectsNotificationApiWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications/queue/add");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void allowsNotificationApiWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications/queue/add");
        request.addHeader("X-PayKesho-Internal-Token", "secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        SecurityContextHolder.clearContext();
    }
}
