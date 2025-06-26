package org.paykesho.organizationportal.security;


import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.commons.entity.organization.OrganizationAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private OrganizationAdminService organizationAdminService;

    @Autowired
    public CustomLoginFailureHandler(@Lazy OrganizationAdminService organizationAdminService) {
        this.organizationAdminService = organizationAdminService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByEmail(email);

        if (organizationAdmin != null) {
            if (organizationAdmin.isEnabled() && organizationAdmin.isAccountNonLocked()) {
                if (organizationAdmin.getFailedAttempt() < OrganizationAdminService.MAX_FAILED_ATTEMPTS - 1) {
                    organizationAdminService.increaseFailedAttempts(organizationAdmin);
                } else {
                    organizationAdminService.lock(organizationAdmin);
                    exception = new LockedException("Your account has been locked due to 3 failed attempts. It will be unlocked after 24 hours.");
                }
            } else if (!organizationAdmin.isAccountNonLocked()) {
                if (organizationAdminService.unlockWhenTimeExpired(organizationAdmin)) {
                    exception = new LockedException("Your account has been unlocked. Please try to login again.");
                }
            }
        }
        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }

}
