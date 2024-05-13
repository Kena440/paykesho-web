package org.chaz.portal.assessors.controller;

import org.chaz.membershipsite.config.Utility;
import org.chaz.membershipsite.memberadmins.service.AssessorService;
import org.chaz.membershipsite.security.AssessorUserDetails;
import org.chaz.membershipsite.security.oauth.AssessorOAuth2User;
import org.chaz.membershipsite.settings.service.SettingService;
import org.membershipcommons.entity.Assessor;
import org.membershipcommons.entity.settings.Country;
import org.membershipcommons.utils.AssessmentToolError;
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

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Controller
public class AssessorController {
    @Autowired
    private AssessorService assessorService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/sign-up-customer")
    public String showSignUpForm(Model model) {
        List<Country> countryList = assessorService.countryList();
        Assessor assessor = new Assessor();
        assessor.setEnabled(false);

        model.addAttribute("countryList", countryList);
        model.addAttribute("pageTitle", "Sign Up");
        model.addAttribute("customer", new Assessor());
        return "register/signup_client";
    }

    @PostMapping(value = "/signup-customer")
    public String signUpAssessor(Assessor assessor,
                                    RedirectAttributes redirectAttributes,
                                    Model model,
                                    HttpServletRequest request) throws IOException, AssessmentToolError, MessagingException {
        assessorService.registerAssessor(assessor);
        //sendVerificationEmail(request, assessor);

        model.addAttribute("pageTitle", "Registration Successful");
        return "register/register_success";
    }

    @PostMapping(value = "/customers/update-account")
    public String updateAccount(Assessor assessor,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        assessorService.updateAssessor(assessor);
        updateNameForAuthenticatedAssessor(assessor, request);
        redirectAttributes.addFlashAttribute("message", "Account update successful");

        return "redirect:/";
    }

    @GetMapping("/customers/verify")
    public String verifyAssessor(@Param("code") String code, Model model) {
        boolean verified = assessorService.verifyAssessor(code);

        return "register/" + (verified ? "verify_success" : "verify_fail");
    }

    @GetMapping("/account")
    public String viewAssessorAccount(Model model,
                                         HttpServletRequest httpServletRequest) {

        String pageTitle = "My Account";
        List<Country> countryList = assessorService.countryList();

        String email = Utility.getEmailOfAuthenticatedAssessor(httpServletRequest);
        Assessor assessor = assessorService.getAssessorByEmail(email);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("countryList", countryList);
        model.addAttribute("customer", assessor);
        return "admins/account";
    }

    private void updateNameForAuthenticatedAssessor(Assessor assessor, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            AssessorUserDetails assessorUserDetails = getAssessorUserDetailsObject(principal);
            Assessor authAssessor = assessorUserDetails.getAssessor();
            authAssessor.setName(assessor.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            AssessorOAuth2User customerOAuth2User = (AssessorOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String customerFullName = assessor.getFullName();
            customerOAuth2User.setFullName(customerFullName);
        }
    }

    private AssessorUserDetails getAssessorUserDetailsObject(Object principal) {
        AssessorUserDetails assessorUserDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            assessorUserDetails = (AssessorUserDetails) token.getPrincipal();
        } else if (principal instanceof RememberMeAuthenticationToken) {
            RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal;
        }
        return assessorUserDetails;
    }

/*
    private void sendVerificationEmail(HttpServletRequest request, Assessor assessor) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = assessor.getEmail();
        String subject = emailSettingBags.getAssessorVerificationSubject();
        String content = emailSettingBags.getAssessorVerificationContent();

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
