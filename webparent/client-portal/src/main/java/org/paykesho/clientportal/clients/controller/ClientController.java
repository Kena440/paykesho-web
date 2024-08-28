package org.paykesho.clientportal.clients.controller;

import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.commons.enums.ClientType;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.organizations.service.OrganizationService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.security.oauth.ClientOAuth2User;
import org.paykesho.clientportal.settings.email.EmailSettingBag;
import org.paykesho.clientportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Client client;

        if (principal instanceof UserDetails) {
            String paykeshoId = ((ClientUserDetails) principal).getCode();
            client = clientService.getClientByCode(paykeshoId);
            model.addAttribute("client", client);
        }

        List<Merchant> merchantList = merchantService.listAllEnabledMerchants();
        model.addAttribute("merchantList", merchantList);
        model.addAttribute("pageTitle", "Dashboard");
        return "clients/dashboard";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        List<Country> countryList = clientService.countryList();
        List<Organization> organizationList = organizationService.organizationListEnabled();
        Client client = new Client();
        client.setEnabled(false);
        client.setClientType(ClientType.FORMAL);
        client.setAccountNonLocked(true);

        model.addAttribute("countryList", countryList);
        model.addAttribute("organizationList", organizationList);
        model.addAttribute("pageTitle", "Client Registration");
        model.addAttribute("client", client);
        return "register/register_form";
    }

    @PostMapping(value = "/save-client")
    public String signUpClient(Client client, Model model, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {

        clientService.registerClient(client);
        sendVerificationEmail(request, client);

        model.addAttribute("pageTitle", "Registration Successful");
        return "register/register_success";
    }

    @PostMapping(value = "/update-account")
    public String updateAccount(Client client, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        clientService.updateClient(client);
        updateNameForAuthenticatedClient(client, request);
        redirectAttributes.addFlashAttribute("message", "Account update successful");

        return "redirect:/";
    }

    @GetMapping("/verify")
    public String verifyClient(@Param("code") String code, Model model) throws PaykeshoError {
        boolean verified = clientService.verifyClient(code);

        return "register/" + (verified ? "verify_success" : "verify_fail");
    }

    @GetMapping("/account")
    public String viewClientAccount(Model model, HttpServletRequest httpServletRequest) {

        String pageTitle = "My Account";
        List<Country> countryList = clientService.countryList();

        String email = Utility.getEmailOfAuthenticatedClient(httpServletRequest);
        Client client = clientService.getClientByEmail(email);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("countryList", countryList);
        model.addAttribute("client", client);
        return "admins/account";
    }

    private void updateNameForAuthenticatedClient(Client client, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            ClientUserDetails ClientUserDetails = getClientUserDetailsObject(principal);
            Client authClient = ClientUserDetails.getClient();
            authClient.setName(client.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            ClientOAuth2User clientOAuth2User = (ClientOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String clientFullName = client.getFullName();
            clientOAuth2User.setFullName(clientFullName);
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

    private void sendVerificationEmail(HttpServletRequest request, Client client) throws MessagingException, UnsupportedEncodingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = client.getEmail();
        String subject = emailSettingBags.getClientVerificationSubject();
        String content = emailSettingBags.getClientVerificationContent();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", client.getFullName());
        String verifyURL = Utility.getSiteUrl(request) + "/clients/verify?code=" + client.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }
}
