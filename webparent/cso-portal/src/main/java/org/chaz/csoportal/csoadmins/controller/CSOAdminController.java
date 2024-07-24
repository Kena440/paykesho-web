package org.chaz.csoportal.csoadmins.controller;

import org.chaz.csoportal.csoadmins.service.CSOAdminService;
import org.chaz.csoportal.config.Utility;
import org.chaz.csoportal.security.CSOAdminUserDetails;
import org.chaz.csoportal.security.oauth.CSOAdminOAuth2User;
import org.chaz.csoportal.settings.service.SettingService;
import org.commons.entity.cso.CSOAdmin;
import org.commons.entity.settings.Country;
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
public class CSOAdminController {
    @Autowired
    private CSOAdminService csoAdminService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/sign-up-customer")
    public String showSignUpForm(Model model) {
        List<Country> countryList = csoAdminService.countryList();
        CSOAdmin assessor = new CSOAdmin();
        assessor.setEnabled(false);

        model.addAttribute("countryList", countryList);
        model.addAttribute("pageTitle", "Sign Up");
        model.addAttribute("customer", new CSOAdmin());
        return "register/signup_client";
    }

    @PostMapping(value = "/signup-customer")
    public String signUpCSOAdmin(CSOAdmin assessor,
                                 RedirectAttributes redirectAttributes,
                                 Model model,
                                 HttpServletRequest request) {
        csoAdminService.registerCSOAdmin(assessor);
        //sendVerificationEmail(request, assessor);

        model.addAttribute("pageTitle", "Registration Successful");
        return "register/register_success";
    }

    @PostMapping(value = "/cso-admins/update-account")
    public String updateAccount(CSOAdmin assessor,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        csoAdminService.updateCSOAdmin(assessor);
        updateNameForAuthenticatedCSOAdmin(assessor, request);
        redirectAttributes.addFlashAttribute("message", "Account update successful");

        return "redirect:/";
    }

    @GetMapping("/customers/verify")
    public String verifyCSOAdmin(@Param("code") String code, Model model) {
        boolean verified = csoAdminService.verifyCSOAdmin(code);

        return "register/" + (verified ? "verify_success" : "verify_fail");
    }

    @GetMapping("/account")
    public String viewCSOAdminAccount(Model model,
                                      HttpServletRequest httpServletRequest) {

        String pageTitle = "My Account";
        List<Country> countryList = csoAdminService.countryList();

        String email = Utility.getEmailOfAuthenticatedCSOAdmin(httpServletRequest);
        CSOAdmin assessor = csoAdminService.getCSOAdminByEmail(email);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("countryList", countryList);
        model.addAttribute("customer", assessor);
        return "admins/account";
    }

    private void updateNameForAuthenticatedCSOAdmin(CSOAdmin assessor, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            CSOAdminUserDetails CSOAdminUserDetails = getCSOAdminUserDetailsObject(principal);
            CSOAdmin authCSOAdmin = CSOAdminUserDetails.getCSOAdmin();
            authCSOAdmin.setName(assessor.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            CSOAdminOAuth2User customerOAuth2User = (CSOAdminOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String customerFullName = assessor.getFullName();
            customerOAuth2User.setFullName(customerFullName);
        }
    }

    private CSOAdminUserDetails getCSOAdminUserDetailsObject(Object principal) {
        CSOAdminUserDetails CSOAdminUserDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            CSOAdminUserDetails = (CSOAdminUserDetails) token.getPrincipal();
        } else if (principal instanceof RememberMeAuthenticationToken) {
            RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal;
        }
        return CSOAdminUserDetails;
    }

/*
    private void sendVerificationEmail(HttpServletRequest request, CSOAdmin assessor) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = assessor.getEmail();
        String subject = emailSettingBags.getCSOAdminVerificationSubject();
        String content = emailSettingBags.getCSOAdminVerificationContent();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", assessor.getFullName());
        String verifyURL = Utility.getSiteUrl(request) + "/customers/verify?code=" + assessor.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }
*/
}
