package org.paykesho.organizationportal.security.oauth;

import org.commons.entity.organization.OrganizationAdmin;
import org.commons.enums.AuthenticationType;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
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
    private OrganizationAdminService OrganizationAdminService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OrganizationAdminOAuth2User oAuth2User = (OrganizationAdminOAuth2User) authentication.getPrincipal();

        String name = oAuth2User.getName();
        String email = oAuth2User.getEmail();
        String countryCode = request.getLocale().getCountry();
        String clientName = oAuth2User.getClientName();

        AuthenticationType authenticationType = getAuthenticationType(clientName);

        OrganizationAdmin assessor = OrganizationAdminService.getOrganizationAdminByEmail(email);

        if (assessor == null) {
            OrganizationAdminService.addNewOrganizationAdminUponOAuthLogin(name, email, countryCode, authenticationType);
        } else {
            oAuth2User.setFullName(assessor.getFullName());
            OrganizationAdminService.updateAuthenticationType(assessor, authenticationType);
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
