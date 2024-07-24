package org.chaz.csoportal.security.oauth;

import org.chaz.csoportal.csoadmins.service.CSOAdminService;
import org.commons.entity.cso.CSOAdmin;
import org.commons.enums.AuthenticationType;
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
    private CSOAdminService CSOAdminService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CSOAdminOAuth2User oAuth2User = (CSOAdminOAuth2User) authentication.getPrincipal();

        String name = oAuth2User.getName();
        String email = oAuth2User.getEmail();
        String countryCode = request.getLocale().getCountry();
        String clientName = oAuth2User.getClientName();

        AuthenticationType authenticationType = getAuthenticationType(clientName);

        CSOAdmin assessor = CSOAdminService.getCSOAdminByEmail(email);

        if (assessor == null) {
            CSOAdminService.addNewCSOAdminUponOAuthLogin(name, email, countryCode, authenticationType);
        } else {
            oAuth2User.setFullName(assessor.getFullName());
            CSOAdminService.updateAuthenticationType(assessor, authenticationType);
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
