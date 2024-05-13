package org.chaz.membershipsite.security.oauth;

import org.chaz.membershipsite.memberadmins.service.MemberAdminService;
import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.enums.AuthenticationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private MemberAdminService memberAdminService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        MemberAdminOAuth2User oAuth2User = (MemberAdminOAuth2User) authentication.getPrincipal();

        String name = oAuth2User.getName();
        String email = oAuth2User.getEmail();
        String countryCode = request.getLocale().getCountry();
        String clientName = oAuth2User.getClientName();

        AuthenticationType authenticationType = getAuthenticationType(clientName);

        MemberAdmin memberAdmin = memberAdminService.getMemberAdminByEmail(email);

        if (memberAdmin == null) {
            memberAdminService.addNewMemberAdminUponOAuthLogin(name, email, countryCode, authenticationType);
        } else {
            oAuth2User.setFullName(memberAdmin.getFullName());
            memberAdminService.updateAuthenticationType(memberAdmin, authenticationType);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private AuthenticationType getAuthenticationType(String clientName) {
        if (clientName.equals("Google")) {
            return AuthenticationType.GOOGLE;
        } else if (clientName.equals("Facebook")) {
            return AuthenticationType.FACEBOOK;
        } else {
            return AuthenticationType.DATABASE;
        }
    }
}
