package org.paykesho.clientportal.clients.controller;

import org.commons.entity.client.Client;
import org.commons.entity.settings.Country;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.security.oauth.ClientOAuth2User;
import org.paykesho.clientportal.settings.service.SettingService;
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
public class ClientController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/sign-up-customer")
    public String showSignUpForm(Model model) {
        List<Country> countryList = clientService.countryList();
        Client assessor = new Client();
        assessor.setEnabled(false);

        model.addAttribute("countryList", countryList);
        model.addAttribute("pageTitle", "Sign Up");
        model.addAttribute("customer", new Client());
        return "register/signup_client";
    }

    @PostMapping(value = "/signup-customer")
    public String signUpClient(Client assessor,
                               RedirectAttributes redirectAttributes,
                               Model model,
                               HttpServletRequest request) {
        clientService.registerClient(assessor);
        //sendVerificationEmail(request, assessor);

        model.addAttribute("pageTitle", "Registration Successful");
        return "register/register_success";
    }

    @PostMapping(value = "/cso-admins/update-account")
    public String updateAccount(Client assessor,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        clientService.updateClient(assessor);
        updateNameForAuthenticatedClient(assessor, request);
        redirectAttributes.addFlashAttribute("message", "Account update successful");

        return "redirect:/";
    }

    @GetMapping("/customers/verify")
    public String verifyClient(@Param("code") String code, Model model) {
        boolean verified = clientService.verifyClient(code);

        return "register/" + (verified ? "verify_success" : "verify_fail");
    }

    @GetMapping("/account")
    public String viewClientAccount(Model model,
                                    HttpServletRequest httpServletRequest) {

        String pageTitle = "My Account";
        List<Country> countryList = clientService.countryList();

        String email = Utility.getEmailOfAuthenticatedClient(httpServletRequest);
        Client assessor = clientService.getClientByEmail(email);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("countryList", countryList);
        model.addAttribute("customer", assessor);
        return "admins/account";
    }

    private void updateNameForAuthenticatedClient(Client assessor, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            ClientUserDetails ClientUserDetails = getClientUserDetailsObject(principal);
            Client authClient = ClientUserDetails.getClient();
            authClient.setName(assessor.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            ClientOAuth2User customerOAuth2User = (ClientOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String customerFullName = assessor.getFullName();
            customerOAuth2User.setFullName(customerFullName);
        }
    }

    private ClientUserDetails getClientUserDetailsObject(Object principal) {
        ClientUserDetails ClientUserDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            ClientUserDetails = (ClientUserDetails) token.getPrincipal();
        } else if (principal instanceof RememberMeAuthenticationToken) {
            RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal;
        }
        return ClientUserDetails;
    }

/*
    private void sendVerificationEmail(HttpServletRequest request, Client assessor) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = assessor.getEmail();
        String subject = emailSettingBags.getClientVerificationSubject();
        String content = emailSettingBags.getClientVerificationContent();

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
