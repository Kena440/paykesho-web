package org.paykesho.organizationportal.organizationadmins.controller;

import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.organizationportal.config.Utility;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.security.OrganizationAdminUserDetails;
import org.paykesho.organizationportal.security.oauth.OrganizationAdminOAuth2User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class OrganizationAdminController {

    private static final Logger log = LoggerFactory.getLogger(OrganizationAdminController.class);

    @Autowired
    private OrganizationAdminService organizationAdminService;


    @GetMapping("/sign-up-customer")
    public String showSignUpForm(Model model) {
        List<Country> countryList = organizationAdminService.countryList();
        OrganizationAdmin assessor = new OrganizationAdmin();
        assessor.setEnabled(false);

        model.addAttribute("countryList", countryList);
        model.addAttribute("pageTitle", "Sign Up");
        model.addAttribute("customer", new OrganizationAdmin());
        return "register/signup_client";
    }

    @PostMapping(value = "/signup-customer")
    public String signUpOrganizationAdmin(OrganizationAdmin assessor,
                                          RedirectAttributes redirectAttributes,
                                          Model model,
                                          HttpServletRequest request) {
        try {
            log.debug("Processing organization admin signup for email: {}", assessor.getEmail());

            organizationAdminService.registerOrganizationAdmin(assessor);
            //sendVerificationEmail(request, assessor);

            model.addAttribute("pageTitle", "Registration Successful");
            log.debug("Successfully registered organization admin");
            return "register/register_success";

        } catch (Exception ex) {
            log.error("Error during organization admin signup: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Registration failed. Please try again later.");
            model.addAttribute("pageTitle", "Registration Failed");
            return "error/500";
        }
    }


    @GetMapping("/customers/verify")
    public String verifyOrganizationAdmin(@Param("code") String code, Model model) {
        boolean verified = organizationAdminService.verifyOrganizationAdmin(code);

        return "register/" + (verified ? "verify_success" : "verify_fail");
    }


    private void updateNameForAuthenticatedOrganizationAdmin(OrganizationAdmin assessor, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            OrganizationAdminUserDetails OrganizationAdminUserDetails = getOrganizationAdminUserDetailsObject(principal);
            OrganizationAdmin authOrganizationAdmin = OrganizationAdminUserDetails.getOrganizationAdmin();
            authOrganizationAdmin.setName(assessor.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            OrganizationAdminOAuth2User customerOAuth2User = (OrganizationAdminOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String customerFullName = assessor.getFullName();
            customerOAuth2User.setFullName(customerFullName);
        }
    }

    private OrganizationAdminUserDetails getOrganizationAdminUserDetailsObject(Object principal) {
        OrganizationAdminUserDetails OrganizationAdminUserDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            OrganizationAdminUserDetails = (OrganizationAdminUserDetails) token.getPrincipal();
        } else if (principal instanceof RememberMeAuthenticationToken) {
            RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal;
        }
        return OrganizationAdminUserDetails;
    }

/*
    private void sendVerificationEmail(HttpServletRequest request, OrganizationAdmin assessor) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = assessor.getEmail();
        String subject = emailSettingBags.getOrganizationAdminVerificationSubject();
        String content = emailSettingBags.getOrganizationAdminVerificationContent();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", assessor.getFullName());
        String verifyURL = Utility.getSiteUrl(request) + "/customers/verify?code=" + assessor.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        //mailSender.send(mimeMessage);
    }
*/
}
