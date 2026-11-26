package org.paykesho.fileservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;

import static org.junit.jupiter.api.Assertions.*;

class InternalFileServiceTokenFilterTest {
    private InternalFileServiceTokenFilter filter() {
        return new InternalFileServiceTokenFilter("secret-token");
    }

    @Test
    void rejectsPrivateFileAccessWithoutInternalToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/payslip/a.pdf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void allowsPublicLogoReadWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/logo/logo.png");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void allowsInternalUploadWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/uploads-api");
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
