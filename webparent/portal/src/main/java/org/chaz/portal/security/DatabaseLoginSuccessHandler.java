package org.chaz.membershipsite.security;

import org.chaz.membershipsite.memberadmins.service.MemberAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class DatabaseLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private MemberAdminService memberAdminService;
/*
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        MemberAdminUserDetails memberAdminUserDetails = (MemberAdminUserDetails) authentication.getPrincipal();
        MemberAdmin memberAdmin = memberAdminUserDetails.getMemberAdmin();

        memberAdminService.updateAuthenticationType(memberAdmin, AuthenticationType.DATABASE);
        if (memberAdmin.getFailedAttempt() > 0) {
            memberAdminService.resetFailedAttempts(memberAdmin.getEmail());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }*/
}
