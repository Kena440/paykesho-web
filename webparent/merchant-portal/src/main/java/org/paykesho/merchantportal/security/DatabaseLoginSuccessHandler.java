package org.paykesho.merchantportal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class DatabaseLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLoginSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        
        String email = authentication.getName();
        LOGGER.info("Successful authentication for merchant admin: {}", email);
        
        // Set default target URL to redirect to dashboard
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
        
        LOGGER.debug("Redirecting merchant admin {} to dashboard", email);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
