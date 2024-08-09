package org.paykesho.merchantportal.merchantadmins.controller;

import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.merchantportal.config.Utility;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.paykesho.merchantportal.security.MerchantAdminUserDetails;
import org.paykesho.merchantportal.security.oauth.MerchantAdminOAuth2User;
import org.paykesho.merchantportal.settings.service.SettingService;
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
public class MerchantAdminController {
    @Autowired
    private MerchantAdminService merchantAdminService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/sign-up-customer")
    public String showSignUpForm(Model model) {
        List<Country> countryList = merchantAdminService.countryList();
        MerchantAdmin assessor = new MerchantAdmin();
        assessor.setEnabled(false);

        model.addAttribute("countryList", countryList);
        model.addAttribute("pageTitle", "Sign Up");
        model.addAttribute("customer", new MerchantAdmin());
        return "register/signup_client";
    }

    @PostMapping(value = "/signup-customer")
    public String signUpMerchantAdmin(MerchantAdmin assessor,
                                      RedirectAttributes redirectAttributes,
                                      Model model,
                                      HttpServletRequest request) {
        merchantAdminService.registerMerchantAdmin(assessor);
        //sendVerificationEmail(request, assessor);

        model.addAttribute("pageTitle", "Registration Successful");
        return "register/register_success";
    }

    @PostMapping(value = "/cso-admins/update-account")
    public String updateAccount(MerchantAdmin assessor,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        merchantAdminService.updateMerchantAdmin(assessor);
        updateNameForAuthenticatedMerchantAdmin(assessor, request);
        redirectAttributes.addFlashAttribute("message", "Account update successful");

        return "redirect:/";
    }

    @GetMapping("/customers/verify")
    public String verifyMerchantAdmin(@Param("code") String code, Model model) {
        boolean verified = merchantAdminService.verifyMerchantAdmin(code);

        return "register/" + (verified ? "verify_success" : "verify_fail");
    }

    @GetMapping("/account")
    public String viewMerchantAdminAccount(Model model,
                                           HttpServletRequest httpServletRequest) {

        String pageTitle = "My Account";
        List<Country> countryList = merchantAdminService.countryList();

        String email = Utility.getEmailOfAuthenticatedMerchantAdmin(httpServletRequest);
        MerchantAdmin assessor = merchantAdminService.getMerchantAdminByEmail(email);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("countryList", countryList);
        model.addAttribute("customer", assessor);
        return "admins/account";
    }

    private void updateNameForAuthenticatedMerchantAdmin(MerchantAdmin assessor, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            MerchantAdminUserDetails MerchantAdminUserDetails = getMerchantAdminUserDetailsObject(principal);
            MerchantAdmin authMerchantAdmin = MerchantAdminUserDetails.getMerchantAdmin();
            authMerchantAdmin.setName(assessor.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            MerchantAdminOAuth2User customerOAuth2User = (MerchantAdminOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String customerFullName = assessor.getFullName();
            customerOAuth2User.setFullName(customerFullName);
        }
    }

    private MerchantAdminUserDetails getMerchantAdminUserDetailsObject(Object principal) {
        MerchantAdminUserDetails MerchantAdminUserDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            MerchantAdminUserDetails = (MerchantAdminUserDetails) token.getPrincipal();
        } else if (principal instanceof RememberMeAuthenticationToken) {
            RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal;
        }
        return MerchantAdminUserDetails;
    }

/*
    private void sendVerificationEmail(HttpServletRequest request, MerchantAdmin assessor) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = assessor.getEmail();
        String subject = emailSettingBags.getMerchantAdminVerificationSubject();
        String content = emailSettingBags.getMerchantAdminVerificationContent();

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
