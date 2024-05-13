package org.chaz.membershipsite.security;

import org.chaz.membershipsite.memberadmins.service.MemberAdminService;
import org.membershipcommons.entity.members.MemberAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private MemberAdminService memberAdminService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        MemberAdmin memberAdmin = memberAdminService.getMemberAdminByEmail(email);

        if (memberAdmin != null) {
            if (memberAdmin.isEnabled() && memberAdmin.isAccountNonLocked()) {
                if (memberAdmin.getFailedAttempt() < MemberAdminService.MAX_FAILED_ATTEMPTS - 1) {
                    memberAdminService.increaseFailedAttempts(memberAdmin);

                    //todo code for email to user on failed login
                } else {
                    memberAdminService.lock(memberAdmin);
                    exception = new LockedException("Your account has been locked due to 3 failed attempts."
                            + " It will be unlocked after 24 hours.");
                    //todo code for email to user on failed login
                }
            } else if (!memberAdmin.isAccountNonLocked()) {
                if (memberAdminService.unlockWhenTimeExpired(memberAdmin)) {
                    exception = new LockedException("Your account has been unlocked. Please try to login again.");
                    //todo code for email to user on failed login
                }
            }

        }

        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }

}
