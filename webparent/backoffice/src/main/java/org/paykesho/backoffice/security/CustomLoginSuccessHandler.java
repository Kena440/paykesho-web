package org.paykesho.backoffice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLoginSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String email = authentication.getName();
        LOGGER.info("Successful authentication for user: {}", email);
        System.out.println("SUCCESS: Login successful for user: " + email);
        
        // Set the default success URL
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
        
        System.out.println("SUCCESS: Redirecting to dashboard");
        LOGGER.debug("Redirecting user {} to dashboard", email);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
