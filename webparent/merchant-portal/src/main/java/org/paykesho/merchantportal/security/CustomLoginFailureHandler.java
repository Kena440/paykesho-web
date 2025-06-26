package org.paykesho.merchantportal.security;


import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.commons.entity.merchant.MerchantAdmin;
import org.springframework.security.authentication.LockedException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private MerchantAdminService merchantAdminService;

    @Autowired
    public CustomLoginFailureHandler(@Lazy MerchantAdminService merchantAdminService) {
        this.merchantAdminService = merchantAdminService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        MerchantAdmin merchantAdmin = merchantAdminService.getMerchantAdminByEmail(email);

        if (merchantAdmin != null) {
            if (merchantAdmin.isEnabled() && merchantAdmin.isAccountNonLocked()) {
                if (merchantAdmin.getFailedAttempt() < MerchantAdminService.MAX_FAILED_ATTEMPTS - 1) {
                    merchantAdminService.increaseFailedAttempts(merchantAdmin);
                } else {
                    merchantAdminService.lock(merchantAdmin);
                    exception = new LockedException("Your account has been locked due to 3 failed attempts. It will be unlocked after 24 hours.");
                }
            } else if (!merchantAdmin.isAccountNonLocked()) {
                if (merchantAdminService.unlockWhenTimeExpired(merchantAdmin)) {
                    exception = new LockedException("Your account has been unlocked. Please try to login again.");
                }
            }
        }
        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }

}
